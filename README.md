# centralidade

Quatro medidas de centralidade em grafo, do zero em Java 21: grau, proximidade,
intermediação de Brandes e PageRank. Nenhuma biblioteca de grafos.

São **quatro juízes independentes**, e nenhum deles sabe o truque do outro:

1. A **definição** de intermediação, em distâncias de todos para todos e contagem de
   caminhos mínimos. Custa o cubo do número de vértices.
2. A **enumeração literal**: listar todos os caminhos simples entre cada par e contar
   na mão. Custa fatorial e só vai até oito vértices.
3. O **sistema linear** do PageRank, resolvido por eliminação de Gauss. A iteração de
   potência se aproxima da resposta; a eliminação **é** a resposta.
4. As **formas com resposta conhecida**: na estrela o centro tem intermediação 1 e as
   pontas têm 0; no completo a proximidade de todos é 1.

## As quatro medidas discordam sobre quem é o centro

```
$ mvn exec:java -Dexec.args="centro"

  grafo                          vertices   por grau    por proximidade    por intermediacao   por PageRank  as quatro concordam
  caminho de 5                          5          1                  2                    2              1                  nao
  caminho de 9                          9          1                  4                    4              1                  nao
  estrela de 7                          7          0                  0                    0              0                  sim
  ciclo de 6                            6          0                  0                    0              0                  sim
  completo de 5                         5          0                  0                    0              0                  sim
  haltere de 4                          9          3                  4                    4              3                  nao
  haltere de 6                         13          5                  6                    6              5                  nao
  com beco de 6                         6          1                  2                    1              0                  nao
  dirigido fechado de 6                 6          0                  0                    0              0                  sim
```

Em **cinco dos onze** grafos elas apontam para vértices diferentes. Isso não é defeito
de nenhuma delas: são perguntas diferentes. Grau pergunta quem conhece mais gente,
proximidade pergunta quem chega rápido em todo mundo, intermediação pergunta quem é a
ponte e PageRank pergunta para onde um passeio aleatório volta mais.

No caminho de 9, o grau aponta para o vértice 1 porque **todo** vértice do meio tem
grau 2 e ele é o primeiro do empate. As outras três apontam para o 4, que é o meio de
verdade.

No grafo dirigido com beco, as quatro apontam para **três** vértices diferentes.

## A ponte: menor grau, maior intermediação

```
$ mvn exec:java -Dexec.args="haltere"

    por lado   vertices    ponte    grau da ponte   intermediacao da ponte     posto por grau    posto por intermediacao
           3          7        3           0.3333                   0.6000                  5                          1
           4          9        4           0.2500                   0.5714                  9                          1
           6         13        6           0.1667                   0.5455                 13                          1
           8         17        8           0.1250                   0.5333                 17                          1
```

O halter são duas rodas fechadas ligadas por um vértice só. Esse vértice é o **último
colocado em grau** e o **primeiro em intermediação**, e a distância entre as duas
posições cresce com o tamanho: com 8 por lado ele é o 17º de 17 em grau e o 1º em
intermediação.

Quem conhece menos gente é exatamente quem não pode faltar.

## O vazamento do PageRank

```
$ mvn exec:java -Dexec.args="vazamento"

  grafo                        vertices  sem saida    soma com tratamento    soma sem tratamento        vazou      desvio do sistema
  com beco de 4                       4          1           1.0000000000           0.5222684678       0.4777               5.55e-17
  com beco de 6                       6          1           1.0000000000           0.7227042229       0.2773               5.55e-17
  com beco de 10                     10          1           1.0000000000           0.8509259049       0.1491               2.22e-16
  com beco de 20                     20          1           1.0000000000           0.9260866893       0.0739               5.55e-17
  dirigido fechado de 6               6          0           1.0000000000           1.0000000000       0.0000               5.55e-17
```

Um vértice sem saída é um buraco: num passeio aleatório, chegar lá é ficar preso. A
conta ingênua perde a massa que entrou ali e **a soma das notas deixa de ser 1 sem
ninguém avisar**. Num grafo dirigido de 4 vértices com um beco, some 48% da
probabilidade.

O número não é escandaloso porque o grafo é pequeno: com 20 vértices o buraco cai para
7%. Ele é escandaloso porque **nada quebra**. As notas continuam saindo, continuam
ordenáveis e continuam erradas.

## Quantas rodadas a iteração precisa

```
$ mvn exec:java -Dexec.args="convergencia"

     rodadas        desvio do sistema                 soma
           1                6.290e-03         1.0000000000
           5                3.427e-04         1.0000000000
          20                4.912e-08         1.0000000000
          50                8.743e-16         1.0000000000
         200                1.388e-17         1.0000000000
```

Essa medida só existe porque há um juiz **exato**. Sem o sistema linear, o critério de
parada seria "parou de mudar", que é outra coisa: um método pode parar de mudar longe
da resposta.

## O custo de Brandes

```
$ mvn exec:java -Dexec.args="custo"

    vertices    arestas        por Brandes       pela definicao      quantas vezes menos
          10         17                576                 2026                      3.5
          20         42               2650                16436                      6.2
          40         82              10618               129676                     12.2
          60        112              22025               434536                     19.7
```

Medido em **visitas**, nunca em segundos: a integração contínua roda em Linux, Windows
e macOS, e três números de segundos não se comparam.

A definição custa o cubo do número de vértices e não olha para as arestas. Brandes
custa vértices vezes arestas. Em grafo esparso a vantagem cresce com o tamanho, e é
por isso que a coluna da direita sobe.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `definicao`, `enumeracao`, `vazamento`, `convergencia`,
`haltere`, `custo`, `centro` e `tudo`.

## O que as medidas me corrigiram

**O juiz estava errado, não o algoritmo testado.** Brandes e a minha implementação da
definição discordavam em dois grafos sorteados, por 9,26e-03 e 1,88e-02. Não era ruído
de ponto flutuante: era diferença de verdade, na terceira casa.

Eu assumi que Brandes estava errado, porque Brandes é o esperto dos dois e o esperto é
quem costuma errar. Estava ao contrário. A minha definição contava os caminhos mínimos
**dentro** do laço de Floyd e Warshall, e isso conta duas vezes: um caminho que passa
por dois vértices intermediários é somado na iteração de cada um deles. A correção foi
separar as duas contas, primeiro as distâncias e depois os caminhos, em ordem de
distância crescente.

Duas implementações que discordam dizem que **uma** das duas está errada e não dizem
qual. Por isso o repositório ganhou um terceiro juiz: a enumeração literal de todos os
caminhos simples, que custa fatorial, só vai até oito vértices e não tem matriz nem
recorrência para errar.

**Nenhuma forma desenhada à mão podia ter pego esse erro.**

```
$ mvn exec:java -Dexec.args="enumeracao"

  grafo                          vertices   arestas       pares com mais de um caminho      desvio de Brandes    desvio da definicao
  caminho de 8                          8         7                                  0               0.00e+00               0.00e+00
  estrela de 7                          7         6                                  0               0.00e+00               0.00e+00
  haltere de 3                          7         8                                  0               0.00e+00               0.00e+00
  completo de 5                         5        10                                  0               0.00e+00               0.00e+00
  ciclo de 8                            8         8                                  4               0.00e+00               0.00e+00
  dirigido fechado de 8                 8        16                                 24               2.78e-17               0.00e+00
  sorteado de 8 a 0.3 (1)               8        13                                 17               1.11e-16               0.00e+00
  sorteado de 8 a 0.3 (2)               8        20                                 12               2.78e-17               0.00e+00
```

A coluna do meio é a resposta. No caminho, na estrela, no halter e no completo há **um
caminho mínimo só** entre cada par de vértices, e onde há um caminho só, contar certo e
contar errado dão exatamente o mesmo número. As quatro formas que eu tinha escolhido a
mão eram justamente as quatro em que o erro é invisível.

Os grafos sorteados têm de 9 a 17 pares com mais de um caminho mínimo. Foi um deles que
pegou.

**A forma que eu escolhi para separar grau de intermediação não separa no menor
tamanho.** O halter de 3 por lado está na tabela mostrando posto por grau **5 de 7**, e
não 7 de 7 como nos outros três tamanhos.

Numa roda de 3 vértices, cada um tem grau 2 dentro da roda, e a ponte também tem grau
2: ela **empata** e o desempate joga ela para o meio da lista. A inversão limpa só
começa com 4 por lado. A linha ficou na tabela porque ela é a medida de onde a forma
para de funcionar.

**A conferência do centro estava comparando a coisa errada.** A primeira versão da
tabela perguntava se Brandes e a definição escolhiam o mesmo **índice** como vértice
mais central, e dava "não" no ciclo e no completo.

Os dois estavam certos. Num grafo simétrico todos os vértices empatam, e qual índice
sai primeiro do empate é detalhe de implementação, não desacordo. A conferência passou
a comparar o **valor** do máximo.

## Licença

MIT.
