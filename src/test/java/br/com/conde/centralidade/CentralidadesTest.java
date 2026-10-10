package br.com.conde.centralidade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Os testes das QUATRO MEDIDAS, contra respostas que dao para escrever sem rodar nada. */
class CentralidadesTest {

    private final Contador contador = new Contador();

    // ───────────────────────────── grau ─────────────────────────────

    @Test
    @DisplayName("o grau e normalizado por n menos um")
    void oGrauENormalizado() {
        double[] naEstrela = Centralidades.grau(Grafo.estrela(7));

        assertThat(naEstrela[0]).isEqualTo(1.0);

        for (int v = 1; v < 7; v++) assertThat(naEstrela[v]).as("ponta %d", v).isEqualTo(1.0 / 6);

        double[] noCompleto = Centralidades.grau(Grafo.completo(5));

        for (int v = 0; v < 5; v++) assertThat(noCompleto[v]).as("vertice %d", v).isEqualTo(1.0);
    }

    @Test
    @DisplayName("o grau de um grafo de um vertice so e zero e nao divisao por zero")
    void oGrauDeUmVerticeSo() {
        assertThat(Centralidades.grau(Grafo.com("um", 1, false).pronto())).containsExactly(0.0);
    }

    // ───────────────────────────── distancias ─────────────────────────────

    @Test
    @DisplayName("as distancias no caminho sao a diferenca de posicao")
    void asDistanciasNoCaminho() {
        int[] daPonta = Centralidades.distanciasDe(Grafo.caminho(6), 0, contador);

        assertThat(daPonta).containsExactly(0, 1, 2, 3, 4, 5);

        int[] doMeio = Centralidades.distanciasDe(Grafo.caminho(6), 3, contador);

        assertThat(doMeio).containsExactly(3, 2, 1, 0, 1, 2);
    }

    /** Vertice inalcancavel nao e caso de borda: e o caso comum em grafo dirigido. */
    @Test
    @DisplayName("o inalcancavel vem como menos um")
    void oInalcancavelVemComoMenosUm() {
        Grafo g = Grafo.comBeco(5);
        int[] doBeco = Centralidades.distanciasDe(g, 4, contador);

        assertThat(doBeco[4]).isZero();

        for (int v = 0; v < 4; v++) assertThat(doBeco[v]).as("vertice %d", v).isEqualTo(-1);
    }

    /**
     * A busca em largura e ITERATIVA, e tem de aguentar um caminho de duzentos mil
     * vertices. Escrita com recursao, ela estoura a pilha da maquina virtual bem antes
     * disso, e estoura em sistemas diferentes em tamanhos diferentes.
     */
    @Test
    @DisplayName("a busca em largura aguenta profundidade de duzentos mil")
    void aBuscaEmLarguraAguentaProfundidade() {
        int n = 200_000;
        int[] distancia = Centralidades.distanciasDe(Grafo.caminho(n), 0, contador);

        assertThat(distancia[n - 1]).isEqualTo(n - 1);
    }

    // ───────────────────────────── proximidade ─────────────────────────────

    @Test
    @DisplayName("no completo todo mundo tem proximidade um")
    void aProximidadeNoCompleto() {
        double[] prox = Centralidades.proximidade(Grafo.completo(6), contador);

        for (int v = 0; v < 6; v++) assertThat(prox[v]).as("vertice %d", v).isCloseTo(1.0, within(1e-12));
    }

    @Test
    @DisplayName("no caminho a proximidade e simetrica e maxima no meio")
    void aProximidadeNoCaminho() {
        double[] prox = Centralidades.proximidade(Grafo.caminho(9), contador);

        for (int v = 0; v < 9; v++)
            assertThat(prox[v]).as("vertice %d contra o espelho", v).isCloseTo(prox[8 - v], within(1e-12));

        assertThat(Centralidades.maior(prox)).isEqualTo(4);
        assertThat(prox[4]).isGreaterThan(prox[3]).isGreaterThan(prox[0]);
    }

    /**
     * A normalizacao de Wasserman e Faust existe para o grafo DESCONEXO. Sem ela, um
     * vertice num pedacinho isolado ganharia a medida por alcancar pouca gente de
     * perto.
     */
    @Test
    @DisplayName("num grafo desconexo o pedaco pequeno nao ganha a medida")
    void aProximidadeNoDesconexo() {
        // um par isolado e um caminho de cinco, no mesmo grafo
        Grafo g = Grafo.com("desconexo", 7, false)
                .aresta(0, 1)
                .aresta(2, 3).aresta(3, 4).aresta(4, 5).aresta(5, 6)
                .pronto();

        double[] prox = Centralidades.proximidade(g, contador);

        // o vertice do par alcanca um vizinho a distancia um e mais ninguem
        assertThat(prox[0]).isCloseTo(1.0 / 6, within(1e-12));

        // o do meio do caminho de cinco alcanca quatro, e ganha mesmo com distancias maiores
        assertThat(prox[4]).isGreaterThan(prox[0]);
    }

    @Test
    @DisplayName("um vertice isolado tem proximidade zero e nao divisao por zero")
    void aProximidadeDoIsolado() {
        Grafo g = Grafo.com("com isolado", 4, false).aresta(0, 1).aresta(1, 2).pronto();

        assertThat(Centralidades.proximidade(g, contador)[3]).isZero();
    }

    // ───────────────────────────── intermediacao ─────────────────────────────

    @Test
    @DisplayName("na estrela o centro tem intermediacao um e as pontas tem zero")
    void aIntermediacaoNaEstrela() {
        double[] inter = Centralidades.intermediacao(Grafo.estrela(7), contador);

        assertThat(inter[0]).isCloseTo(1.0, within(1e-12));

        for (int v = 1; v < 7; v++) assertThat(inter[v]).as("ponta %d", v).isCloseTo(0.0, within(1e-12));
    }

    @Test
    @DisplayName("no completo ninguem esta no meio de ninguem")
    void aIntermediacaoNoCompleto() {
        double[] inter = Centralidades.intermediacao(Grafo.completo(6), contador);

        for (int v = 0; v < 6; v++) assertThat(inter[v]).as("vertice %d", v).isCloseTo(0.0, within(1e-12));
    }

    /**
     * No CAMINHO ha forma fechada: a intermediacao bruta do vertice i e i vezes
     * (n menos 1 menos i), porque todo par com um de cada lado dele passa por ele, e ha
     * um caminho minimo so entre cada par.
     *
     * <p>E um juiz que nao depende de nenhuma implementacao deste repositorio: da para
     * escrever a resposta antes de rodar.
     */
    @Test
    @DisplayName("no caminho a intermediacao bate com a forma fechada i vezes (n-1-i)")
    void aIntermediacaoNoCaminhoTemFormaFechada() {
        for (int n : new int[] {5, 9, 15, 40}) {
            double[] inter = Centralidades.intermediacao(Grafo.caminho(n), contador);
            double fator = (n - 1.0) * (n - 2) / 2;

            for (int i = 0; i < n; i++)
                assertThat(inter[i]).as("caminho de %d, vertice %d", n, i)
                        .isCloseTo(i * (n - 1.0 - i) / fator, within(1e-12));
        }
    }

    @Test
    @DisplayName("no ciclo todos tem a mesma intermediacao")
    void aIntermediacaoNoCiclo() {
        double[] inter = Centralidades.intermediacao(Grafo.ciclo(8), contador);

        for (int v = 1; v < 8; v++)
            assertThat(inter[v]).as("vertice %d", v).isCloseTo(inter[0], within(1e-12));
    }

    /**
     * O ACHADO DO HALTERE: o vertice da ponte tem o MENOR grau do grafo e a MAIOR
     * intermediacao. Quem conhece menos gente e exatamente quem nao pode faltar.
     *
     * <p>E ele e o PRIMEIRO em intermediacao em todos os tamanhos, mas so e o ULTIMO
     * em grau a partir de quatro por lado. Com tres por lado a roda tem vertices de
     * grau dois tambem, a ponte empata e cai para o quinto lugar de sete.
     */
    @Test
    @DisplayName("no haltere a ponte e a primeira em intermediacao, e a ultima em grau a partir de quatro por lado")
    void aPonteDoHaltereInverteAsDuasMedidas() {
        for (int porLado : new int[] {3, 4, 6, 8}) {
            Grafo g = Grafo.haltere(porLado);
            int ponte = porLado;

            double[] grau = Centralidades.grau(g);
            double[] inter = Centralidades.intermediacao(g, contador);

            assertThat(Juiz.ordenados(inter).indexOf(ponte) + 1)
                    .as("posto por intermediacao no haltere de %d", porLado).isEqualTo(1);

            // a ponte sempre tem o menor grau, empatado ou nao
            for (int v = 0; v < g.vertices(); v++)
                assertThat(grau[ponte]).as("grau da ponte contra o %d no haltere de %d", v, porLado)
                        .isLessThanOrEqualTo(grau[v]);

            if (porLado >= 4)
                assertThat(Juiz.ordenados(grau).indexOf(ponte) + 1)
                        .as("posto por grau no haltere de %d", porLado).isEqualTo(g.vertices());
        }

        // com tres por lado o empate joga a ponte para o meio da lista de graus
        assertThat(Juiz.ordenados(Centralidades.grau(Grafo.haltere(3))).indexOf(3) + 1).isEqualTo(5);
    }

    @Test
    @DisplayName("com dois vertices ou menos a intermediacao e zero e nao divisao por zero")
    void aIntermediacaoDeGrafoMinusculo() {
        assertThat(Centralidades.normalizarIntermediacao(new double[] {5, 5}, 2, false)).containsExactly(0.0, 0.0);
        assertThat(Centralidades.intermediacao(Grafo.caminho(2), contador)).containsExactly(0.0, 0.0);
        assertThat(Centralidades.intermediacao(Grafo.caminho(1), contador)).containsExactly(0.0);
    }

    // ───────────────────────────── pagerank ─────────────────────────────

    @Test
    @DisplayName("com tratamento, a soma das notas e um em todo grafo")
    void oPageRankSomaUm() {
        Grafo.todos().forEach((nome, g) -> {
            double[] nota = Centralidades.pageRank(g, Medidas.AMORTECIMENTO, 200, true);

            assertThat(Centralidades.soma(nota)).as(nome).isCloseTo(1.0, within(1e-9));

            for (int v = 0; v < g.vertices(); v++)
                assertThat(nota[v]).as("%s, vertice %d", nome, v).isPositive();
        });
    }

    /**
     * O VAZAMENTO: sem tratamento, a massa que chega no vertice sem saida some, e a
     * soma das notas deixa de ser um SEM NINGUEM AVISAR.
     */
    @Test
    @DisplayName("sem tratamento, o grafo com beco perde massa e o sem beco nao")
    void oPageRankVazaNoBeco() {
        double[] semTratamento = Centralidades.pageRank(Grafo.comBeco(4), Medidas.AMORTECIMENTO, 200, false);

        assertThat(Centralidades.soma(semTratamento)).isLessThan(0.53).isGreaterThan(0.52);

        double[] fechado = Centralidades.pageRank(Grafo.dirigidoFechado(6), Medidas.AMORTECIMENTO, 200, false);

        assertThat(Centralidades.soma(fechado)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    @DisplayName("quanto maior o grafo com beco, menos ele vaza")
    void oVazamentoDiminuiComOTamanho() {
        double anterior = 1;

        for (int n : new int[] {4, 6, 10, 20}) {
            double vazou = 1 - Centralidades.soma(
                    Centralidades.pageRank(Grafo.comBeco(n), Medidas.AMORTECIMENTO, 200, false));

            assertThat(vazou).as("beco de %d", n).isPositive().isLessThan(anterior);

            anterior = vazou;
        }
    }

    @Test
    @DisplayName("no ciclo todo mundo tem a mesma nota, que e um sobre n")
    void oPageRankNoCiclo() {
        double[] nota = Centralidades.pageRank(Grafo.ciclo(7), Medidas.AMORTECIMENTO, 200, true);

        for (int v = 0; v < 7; v++) assertThat(nota[v]).as("vertice %d", v).isCloseTo(1.0 / 7, within(1e-12));
    }

    @Test
    @DisplayName("com zero rodadas a nota e a uniforme de partida")
    void oPageRankComZeroRodadas() {
        double[] nota = Centralidades.pageRank(Grafo.haltere(4), Medidas.AMORTECIMENTO, 0, true);

        for (int v = 0; v < 9; v++) assertThat(nota[v]).as("vertice %d", v).isEqualTo(1.0 / 9);
    }

    @Test
    @DisplayName("um grafo sem vertice nenhum devolve vetor vazio e nao quebra")
    void oPageRankDeGrafoVazio() {
        assertThat(Centralidades.pageRank(Grafo.com("vazio", 0, false).pronto(), 0.85, 10, true)).isEmpty();
    }

    @Test
    @DisplayName("o maior desempata pelo primeiro indice")
    void oMaiorDesempataPeloPrimeiro() {
        assertThat(Centralidades.maior(new double[] {3, 7, 7, 2})).isEqualTo(1);
        assertThat(Centralidades.maior(new double[] {5})).isZero();
    }

    @Test
    @DisplayName("o contador soma e zera")
    void oContador() {
        Contador c = new Contador();

        c.visitou();
        c.visitou(10);

        assertThat(c.visitas()).isEqualTo(11);
        assertThat(c.toString()).isEqualTo("11 visitas");

        c.zerar();

        assertThat(c.visitas()).isZero();
    }
}
