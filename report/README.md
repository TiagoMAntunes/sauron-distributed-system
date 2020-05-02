# Relatório do projeto Sauron

Sistemas Distribuídos 2019-2020, segundo semestre


## Autores

**Grupo A18**



| Number | Name              | User                                 | Email                                   |
| -------|-------------------|--------------------------------------|-----------------------------------------|
| 86591  | Lourenço Teodoro  | <https://github.com/ISTTeo>          | <lourenco.teodoro@tecnico.ulisboa.pt>   |
| 89448  | Gonçalo Almeida   | <https://github.com/xaleza>          | <goncalo.h.almeida@tecnico.ulisboa.pt>  |
| 89545  | Tiago Antunes     | <https://github.com/TiagoMAntunes>   | <tiago.melo.antunes@tecnico.ulisboa.pt> |


![Lourenço Teodoro](lourenco.png) ![Gonçalo Almeida](goncalo.jpeg) ![Tiago Antunes](tiago.jpeg)


## Melhorias da primeira parte

- [Melhoria dos testes de integração do TrackMatch](https://github.com/tecnico-distsys/A18-Sauron/commit/fb83182441b60b4742b162c0b7b2be4061310feb)
- [Melhoria dos testes de integração do CamJoin](https://github.com/tecnico-distsys/A18-Sauron/commit/a42d8495c6b73cbdbd72b115c5493ef352faa96c)
- [Adição de comentários](https://github.com/tecnico-distsys/A18-Sauron/commit/19dc37d5227098c1bdfa5755e23c09fd86e008d8)


## Modelo de faltas

Seguindo o modelo de faltas descrito no enunciado, o nosso sistema tolera então faltas silenciosas e transientes. Uma vez que os gestores de réplica não podem falhar arbitrariamente, não existem falhas bizantinas, pelo que não são consideradas na nossa implementação. Ainda dentro de faltas transientes, o sistema é então tolerante a comportamento assíncrono, além da implementação com HTTP/2, através da interrupção da tentativa de envio de mensagem após X segundos para voltar a reenviar. Desta forma, volta a enviar o pedido e consegue garantir que se reconecta a outra réplica caso uma delas esteja a demorar muito a responder ou tenha perdido a sua ligação (Sistema de faltas dado afirma ter uma réplica ativa a atender os cliente). A tentativa de reenvio de informação é realizada pelo HTTP/2, mas caso este diga que não está disponível, o cliente tenta ainda enviar de novo um número arbitrário de vezes (assumiu-se 5 como um valor razoável).

O modelo de faltas pode-se então resumir a que o sistema é tolerante a f faltas, sendo que existem f+1 réplicas no total. No nosso caso, como só aguenta até 9 réplicas, aguentam-se até 8 faltas. 

No caso de ocorrer omissão de mensagens por parte do canal de comunicação, este problema é resolvido através do uso de Timestamp Vetorial por parte do sistema, o que evita este caso de omissão de mensagens. As faltas são tratadas no momento, não havendo faltas densas. Como o modelo de faltas não aceita faltas bizantinas, assume que não há corrupção dos dados por parte do canal de transmissão (o uso de HTTP/2 também evita grande parte dos erros deste caso).

Uma falta não tratada neste caso é: em caso de todas as tentativas do cliente não ocorrer uma resposta mas, numa delas, o servidor ter enviado resposta que se tenha perdido pelo caminho, ocorrerá uma falha de coêrencia. Neste caso, visto que o Vector Clock dos pedidos vai ser diferente, a falta não é tolerada, pois não há forma de distinguir as observações das câmaras por poderem ter sido feitas sequencialmente. O modelo não é portanto tolerante a este caso.

O cliente SiloFrontend é ainda inteligente e tem um sistema de *cache* para evitar leituras incoerentes por parte do cliente, até ao tamanho da *cache*. Esta funciona através da relação <Pedido, Resposta>, sendo que alguma lógica poderá não estar totalmente correta, apenas se tem a garantia de que pedidos iguais irão dar a mesma resposta caso os dados estejam desatualizados, dentro de tempo útil. Esta ideia veio motivada do facto de os clientes de leituras são humanos, logo a quantidade de pedidos que vão efetuar irá ser reduzida relativamente à velocidade de propagação dos dados. O modelo de faltas inclui ainda que estas são não definitivas, logo eventualmente o servidor irá reconectar-se, o que passará a ter leituras coerentes eventualmente. O tamanho da *cache* do cliente spotter pode ainda ser especificada à mão, podendo-se adaptar ao dispositivo onde for utilizado. 

O SiloFrontend é ainda inteligente relativamente ao comando spotter. Caso tenha ocorrido uma falha de ligação à réplica e houver reconexão mas a informação da câmara não tenha sido registada, este regista a câmara e volta a enviar o pedido, tornando o comportamento transparente para o cliente.


## Solução
![solution](solution.jpg)

A ideia base da solução é termos réplicas que comunicam entre si para garantir a disponibilidade do sistema. Cada réplica comunica as alterações que recebeu dos clientes às outras réplicas, não havendo necessidade de coerência forte, transmitindo as alterações _Log Elements_. Quando o servidor decide fazer gossip, ele envia para todas as réplicas que encontrar visto termos um número reduzido de réplicas (9).

Os clientes comunicam através de um frontend que acaba por tornar transparente o funcionamento da comunicação para quem desenvolve a lógica da aplicação, tratanto de garantir, em melhor esforço, coẽrencia do ponto de vista do cliente.

Explicamos a baixo com mais detalhe o protocolo utilizado.

## Protocolo de replicação
O protocolo utilizado é um protocolo de coêrencia fraca com um suplemento de coerência no *frontend* do client.

A ideia do protocolo é simples e baseia-se no conceito de TimeStamps (referenciados no código como VectorClocks) estes objectos permitem uma noção de temporal entre acontecimentos sem recorrer a relógios (que não são fiáveis para sistemas distribuidos).

Recorrendo a estes timestamps podemos ter uma noção da ordem entre atualizações no servidor.

Cada servidor passa agora a manter dois timestamps, um referente às atualizações aplicadas ao estado, *ValueTS*, e outro referente às atualizações que estão presentes no seu log, *ReplicaTS*. 

Há ainda um *log*, que é uma estrutura utilizada para manter atualizações recebidas quer dos clientes quer de outras réplicas e é este que é consultado aquando das mensagens de *Gossip* a partir deste que se actualiza o estado do servidor.

Estas mensagens de *Gossip* começam por pedir a cada réplica destino o seu *timestamp* de modo a comparar ao *ReplicaTS* e ver que actualizaçṍes necessita o destino. Com base nisso ocorre um filtro do *log* e enviam-se apenas as atualizações necessárias.

Ao receber atualizações pelo *Gossip* estas são adicionadas ao *log* caso não sejam duplicadas de atualizações já existentes no *log*, o *ReplicaTS* é actualizado e, por fim, actualiza-se o estado se tal for possível.

Uma actualização poderá ser aplicada ao estado do servidor quando todas as atualizações que a precedem já foram aplicadas, ou seja, quando o timestamp referente ao estado do servidor indica que não existem *gaps* lógicos entre a actualização e o estado actual do servidor (*ValueTS* >= *timestamp do pedido*).

Para este efeito procedemos a alterações nas mensagens já existentes (maioritariamente a passagem de timestamps nas mensagens de Report) e à criação de dois novos serviços GRPC:

**GetReplicaTimestamp** - Permite pedir a uma replica o seu timestamp para conseguirmos decidir que atualizações necessita;

**Gossip** - Mensagem fulcral para o processo de disseminação de atualizações enviando as atualizações necessárias para outras réplicas e permitindo a actualização dos seus estados.

Criámos ainda a estrutura auxiliar para guardar os _logs_ da réplica, que tem o nome _log_.

O suplemento para aumento da coerência mencionado resume-se à introdução de uma *cache* no frontend dos clientes. 
No caso em que um cliente fazer uma leitura a um servidor e que depois, por alguma razão, faça a mesma leitura a um outro servidor não actualizado não receberá a leitura desatualizada mas sim aquela que recebeu inicialmente. Para isto é necessário que o *Frontend* compare o seu *timestamp* ao da réplica e caso seja inferior vá buscar o valor que tinha guardado na *cache*. 

## Opções de implementação

Na criação da *cache* assumimos como default um tamanho suficiente para não ocorrerem erros de coerência. Inicialmente consideramos replicar o estado do servidor no frontend mas após uma conversa com os professores chegamos à conclusão que tal seria algo excessivo para o fim pretendido.

Como referido acima no modelo de faltas demos preferência à simplicidade da *cache*, baseando nos padrões que usamos para simplificar o *frontend* esta guarda apenas pares pedido/resposta (*Request*/*Message*). Tivemos de implementar o método de comparação entre *Request* de modo a que esta se focasse no objecto observável (*Observable*). Recorremos a dois *maps* de modo a garantir não só conseguir obter as respostas através dos pedidos mas também, caso a *cache* chegue ao seu limite, conseguir substituir os pedidos mais antigos por novos. Para continuar com a *cache* actualizada e manter a unicidade dos pedidos caso tenhamos uma nova resposta para um pedido já presente na cache actualizamos a *cache*, substituindo a resposta antiga.

Para garantir que o sistema estaria capaz de aguentar falhas de réplicas decidimos manter o log sem o limpar, de modo que em caso de necessidade este possa ser totalmente reposto em réplicas que tenham perdido todas as informações. Isto deve-se ao facto de a solução pedida não ter memória persistente. Esta decisão tornou imprescindível o envio de logs de forma selectiva, reduzindo os envios ao estritamente necessário. Caso contrário, o tamanho das mensagens enviadas seria excessivamente grande.

Quando possível optamos pela facilidade de entendimento. Por exemplo, durante o gossip  consideramos percorrer os logs existentes, para cada cada réplica destino (identificada pelo *origin*) ver o *timestamp* mais recente e com base nesse *timestamp* decidir que logs podem estar em falta na réplica.
Consideramos muito mais directa a ideia de enviar mensagens auxiliares com o único propósito de obter o timestamp da réplica destino para depois se proceder à escolha das atualizações a enviar. 


Para a gestão do log, garantimos que este consegue realizar as operações de insert e contains em O(ln(n)), através do uso de uma estrutura `TreeSet`. A procura de elementos para enviar nos logs são realizadas então em O(n), sendo que otimizações poderiam ser feitas aqui mas foi considerado mais relevante garantir a disponibilidade do servidor para responder a pedidos do cliente do que propriamente a propagar as informações.

Consideramos necessário proteger todos os timestamps nas réplicas de acesso concorrentes de modo a evitar problemas em que múltiplas réplicas e clientes tentam comunciar com a réplica em questão. Um dos problemas com que nos deparámos foi, ao correr testes de carga, alguns dos timestamps inseridos nos logs estavam a _saltar_ timestamps. Isto devia-se a que em pedidos muito rápidos, como pediamos os timestamps em 2 locais, um para comparar, e outro para incrementar, sem ser atómico, os valores deixavam de ser coesos e isso causava com que o servidor não conseguisse atualizar os valores pois não havia forma de ter a condição ValueTS >= timestamp do pedido. Desta forma, é possível ver nos pedidos de modificação um pedaço que contenha `synchronized (this.replicaTS)` para evitar este comportamento.



## Notas finais

Tivemos outras ideias, em particular na questão da coerência de frontend. Uma das ideias iniciais era ter o cliente a guardar também logs do servidor, sendo que estes seriam os logs associados às respostas do pedido. Caso houvesse uma troca da réplica com que o frontend se comunica, então estes logs seriam enviados à nova replica para garantir que os pedidos efetuados continuariam a ser coerentes. Decidimos não avançar pelo facto de que caso o cliente efetuasse muitos pedidos, iria necessitar de muita memória, o que não é ideal, e ainda seria necessário adicionar lógica para computar as respostas. Se não fosse, então era preciso fazer uma *cache* semelhante a que foi implementada no fim, mas sem limite de memória, o que continua não ideal. Este caso seria melhor no caso de se ter memória persistente, porque dessa forma era possível ir eliminando logs de memória.