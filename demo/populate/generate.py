import random

def run(ops,file):
    for i,o in enumerate(ops):
        if i % 5 == 0:
            file.write('\nzzz,{:d}\n'.format(random.randint(1000,10000)))
        file.write(o + '\n')
        



ops = []
for i in range(1000):
    for i in range(random.randint(1,10)):
        ops.append("person,{:d}".format(i))

for i in range(100):
    for prefix in ["AAAA",'AAAB','AAAC','AAAD','AAAE']:
        for i in range(random.randint(1,5)):
            ops.append("car,{:s}{:02d}".format(prefix,i))

for i in range(30):
    with open('cam{:03d}.txt'.format(i), 'w+') as f:
        random.shuffle(ops)
        run(ops[:101], f)
