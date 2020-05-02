# Guião de uso

Primeiro deve-se compilar todo o projeto. Da pasta raiz:
```
mvn install -DskipTests
```


Depois, lança-se o servidor:

```
cd silo-server
mvn exec:java
```


O servidor está agora a correr em `localhost:8000` e pode então comunicar com os clientes.

## Demonstração Eye

O cliente Eye liga-se ao servidor e vai automaticamente registar a existência da câmara, podendo-se de seguida enviar observações para o servidor.

Para iniciar o Eye é necessário fazer:
```
./eye/target/appassembler/bin/eye localhost 2181 Tagus 38.737613 -9.303164
```

Neste caso foi criada uma câmara com o nome *Tagus* que está nas coordenas *(38.737613,-9.303164)*.
Esta está agora registada no servidor. Caso esta já existisse, o Eye apenas validava a sua existência.

*Nota: Todos os inputs mostrados estão disponíveis no ficheiro `eye_test.txt`*

Para se enviar observações, basta introduzir um comando do tipo `type,id` em que type é `car` ou `person` e `id` é o identificador de acordo com as especificações do tipo. 
```
# lote 1
person,5638246
car,20SD23

zzz,1000

#lote 2
car,20SD24
```

No servidor foram então criadas três observações, uma para cada uma das entidades. É possível ainda enviar observações repetidas, o que significa que a observação foi realizada pouco tempo depois da outra.

```
# lote 3
person,192837
person,123675
car,AL00AL

zzz,500

# lote 4
person,123675
car,AL00AL
```

Em caso de identificador inválido ou tipo inexistente, o eye irá mostrar uma mensagem de erro. Nestes casos, todas as observações enviadas naquele momento são rejeitadas, mesmo que corretas.
```
# erro 1
person,AA1234

# erro 2
car,9218347

# erro grupo
car,AL00AL
person,129837
car,111111
person,1293132739
```

Temos ainda o comando :

```
zzz,[milliSeconds]
```

Que nos permite pausar o processamento durante os milissegundos passados.

Para enviar blocos enviamos uma linha em branco, ou podemos fazer isto ao terminar a execução do eye (para isso bastou fazer `CTRL+D`).

## Demonstração Spotter
O cliente Spotter permite realizar pesquisas de informação através dos comandos `spot` e `trail`. Possui ainda comandos extras para o uso de testes: `ping`, `clear` e `init`. Em caso de dúvidas da sintaxe dos comandos pode-se correr o comando `help`.
Se o servidor estiver inalcançável, o Spotter irá mostrar uma mensagem de informação e desligar-se.

**Caso não tenha realizado a demonstração do eye como mencionado antes, faça o seguinte comando antes de começar:**
```
./eye/target/appassembler/bin/eye localhost 2181 Tagus 38.737613 -9.303164 < demo/eye_test.txt
```



Para se iniciar o cliente é necessário fazer:
```
./spotter/target/appassembler/bin/spotter localhost 2181
```


*Nota: Todos os inputs mostrados estão disponíveis no ficheiro `demo/spotter_test.txt`*

Pode-se então começar a realizar pesquisas. 

### Spot
O comando spot recebe um tipo e um identificador, mostrando a ocorrência mais recente do objeto encontrado. Em caso de o identificador ser parcial, irá mostrar a ocorrência mais recente de todos os objetos que tenham o identificador parcial.
```
spot person 5638246
spot car 20SD23
spot person 1*
```

Em caso de o objeto com um id não-parcial não existir, é mostrada uma mensagem de erro. 
```
spot person 111111111111111
```

### Trail
O comando trail recebe um tipo e um identificado, mostrando o caminho percorrido pelo objeto referido. Um identificador parcial não irá encontrar nenhuma ocorrência.

```
trail person 123675
trail car AL00AL
trail car 20SD23
```
Um resultado vazio não é erro.

### Help
O comando help mostra quais os comandos que existem e como os correr. Basta escrever:
```
help
```

### Init
O comando init permite colocar diretamente observações no servidor. O comando tem a seguinte sintaxe:
```
init <amount> [type id cam_name latitude longitude]
```
Tudo o que estiver entre `[]` quer dizer que tem de ser repetido `amount` vezes.
Por exemplo:
```
init 1 car AA00AA Tagus 1 1
```
Este comando cria no servidor a câmara Tagus e a observação com do objeto especificado, na hora atual.

### Clear
O comando clear limpa o estado do servidor de volta ao estado inicial. Para isso basta correr:
```
clear
```

### Ping
O comando ping permite verificar se o servidor esta' ligado. Caso esteja ligado, ira' devolver uma mensagem.
```
ping friend
```
Este comando irá devolver `Hello friend!`

### Exit
Permite desligar o spotter. Basta escrever:
```
exit
```

# Teste de carga Eye
Para testar a performance do sistema foi ainda criado um teste de carga de forma a poder simular um caso real de uso e avaliar se o servidor estava suficientemente paralelizado.

Criou-se então um teste que era composto de:
* 100 câmaras
* 1000 pessoas, podento aparecer cada uma até 10x
* 500 veículos, podendo aparecer cada um até 5x
* Cada câmara apenas deteta 500 objetos no total

A ideia  é que as câmaras sendo de baixa potência enviam 5 reconhecimentos de cada vez (um valor aceitável) dando sleep com um valor aleatório entre 1ms e 1s, desta forma populando o servidor com dados que podem depois ser analizados pelo spotter.

Existem então 2 ficheiros: `generate.py` e `run.sh`. O `generate.py` gera as 100 câmaras e o ficheiro `run.sh` executa-as. **Atenção: são lançados 100 processos em paralelo o que poderá ser demasiado para alguns computadores com pouca memória RAM**.



# Demonstração funcionamento da Cache

Vamos demonstrar uma simples utilização da cache do *frontend*. 

## Preparação

Para tal começamos por correr 2 silo-servers, fazendo em 2 terminais:

```
cd silo-server
```

Começamos pelo terminal a que vamos ligar o nosso Eye. De notar que este possui um intervalo de 300 segundos (5 minutos) absurdo propositado para dar tempo para se simular. Pode-se alterar o intervalo para ter mais tempo se for preferível.

```
mvn compile exec:java -Dinstance=1 -Dinterval=300
```

Noutro terminal fazemos:

```
./eye/target/appassembler/bin/eye localhost 2181 Tagus 38.737613 -9.303164
```
O eye está agora ligado ao único servidor disponível.

Inicializemos um spotter associado ao primeiro servidor:

```
./spotter/target/appassembler/bin/spotter localhost 2181
```

## Funcionamento

No eye enviamos uma observacao:

```
CAR,20SD20
```

No spotter pedimos informação relativamente a esta entidade:

```
spot CAR 20SD20
```

Obtendo uma resposta.

Inicializemos agora o segundo servidor num terceiro terminal:

```
mvn compile exec:java -Dinstance=2
```

E façamos a réplica 1 crashar através de `Ctrl-C` no terminal correspondente e voltemos a pedir a informação dentro do spotter. De notar que a réplica tem de ir abaixo antes de efectuar o Gossip com outras réplicas.

No spotter voltemos a efectuar o comando:

```
spot CAR 20SD20
```

E vamos obter na mesma a resposta, apesar de esta não existir. Este comportamento pode ser replicado para os restantes comandos.

# Funcionamento do Gossip

## Preparação

Para tal começamos por correr 2 silo-servers, fazendo em 2 terminais:

```
cd silo-server
```

Começamos pelo terminal a que vamos ligar o nosso Eye:

```
mvn compile exec:java -Dinstance=1
```

Noutro terminal fazemos:

```
./eye/target/appassembler/bin/eye localhost 2181 Tagus 38.737613 -9.303164
```

E por fim corremos o segundo servidor num terceiro terminal:

```
mvn compile exec:java -Dinstance=2
```

Inicializamos um spotter associado a cada servidor:

```
./spotter/target/appassembler/bin/spotter localhost 2181 1
```

```
./spotter/target/appassembler/bin/spotter localhost 2181 2
```

## Funcionamento

No eye enviamos uma observacao:

```
CAR,20SD20
```

No spotter associado à primeira réplica pedimos informação relativamente a esta entidade:

```
spot CAR 20SD20
```

Obtendo uma resposta como esperado. Verificamos agora a segunda réplica através do outro spotter:

```
spot CAR 20SD20
```

Não obtivemos o registo desejado como esperado.

Aguardemos agora 30 segundos e repetimos o pedido na segunda réplica:

```
spot CAR 20SD20
```

Obtivemos agora resposta, provando que a comunicação de actualizações entre réplicas funciona.


# Tolerância a faltas

Vamos agora analisar a tolerância a faltas por parte do servidor.

Inicie-se um servidor:

```
mvn exec:java -Dinterval=10
```

Inicie-se uma instância do eye noutro terminal:

```
./eye/target/appassembler/bin/eye localhost 2181 Tagus 38.737613 -9.303164
```

Envie-se agora uma observação:

```
$ CAR,SD20SD
```

O servidor recebe a informação. Adormeça-se agora o servidor fazendo `ctrl-z` no terminal deste.

Inicie-se agora outro servidor:
```
mvn exec:java -Dinstance=2 -Dinterval=10
```

No eye, tente-se enviar outra observação:
```
$ PERSON,1
```

Ligue-se agora um spotter à replica 2 e peça-se informação sobre a observação enviada:

```
./spotter/target/appassembler/bin/spotter localhost 2181 2
$ spot PERSON 1
```

Repare-se na informação da câmara como desejada.

No terminal da primeira instância, escreva-se agora `fg` para recuperar a informação.

Ligue-se um spotter à replica 1:

```
./spotter/target/appassembler/bin/spotter localhost 2181 1
```

Aguarde-se então que ocorra gossip por parte dos servidores.

Depois, em cada spotter, realize-se o seguinte pedido:

```
$ spot PERSON 1
$ spot CAR 20SD20
```

Os resultados são agora iguais, e o sistema recuperou da falta.

