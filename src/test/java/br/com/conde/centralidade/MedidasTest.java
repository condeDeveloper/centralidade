package br.com.conde.centralidade;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Os testes das TABELAS que o medidor imprime.
 *
 * <p>Toda frase do README que e um numero esta aqui como afirmacao. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
class MedidasTest {

    // ───────────────────── contra a forca bruta ─────────────────────

    /**
     * A tabela de cima do README: em todo grafo, Brandes bate com a definicao e a
     * iteracao bate com o sistema linear.
     *
     * <p>Os dois desvios sao ruido de ponto flutuante e nada mais. Se algum deles
     * subir para a terceira casa, como ja subiu, ha um erro de verdade em alguma das
     * quatro contas.
     */
    @Test
    @DisplayName("em todo grafo os dois desvios ficam na casa do ruido de ponto flutuante")
    void aTabelaContraAForcaBruta() {
        List<Medidas.LinhaDefinicao> linhas = Medidas.contraAForcaBruta();

        assertThat(linhas).hasSize(11);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.desvioDaIntermediacao()).as("intermediacao em %s", l.grafo()).isLessThan(1e-12);
            assertThat(l.desvioDoPageRank()).as("PageRank em %s", l.grafo()).isLessThan(1e-12);
            assertThat(l.somaDoPageRank()).as("soma em %s", l.grafo()).isCloseTo(1.0, within(1e-9));
            assertThat(l.mesmoCentro()).as("centro em %s", l.grafo()).isTrue();
        });
    }

    @Test
    @DisplayName("as formas com resposta conhecida batem todas")
    void aTabelaDasFormasConhecidas() {
        List<Medidas.LinhaConhecida> linhas = Medidas.asFormasConhecidas();

        assertThat(linhas).hasSize(9);
        assertThat(linhas).allSatisfy(l ->
                assertThat(l.bate()).as("%s: %s", l.grafo(), l.medida()).isTrue());

        assertThat(acharConhecida(linhas, "estrela de 7", "intermediacao do centro").obtido()).isEqualTo("1.0000");
        assertThat(acharConhecida(linhas, "estrela de 7", "intermediacao das pontas").obtido()).isEqualTo("0.0000");
        assertThat(acharConhecida(linhas, "completo de 6", "proximidade de todos").obtido()).isEqualTo("1.0000");
    }

    /**
     * A TABELA QUE EXPLICA O ERRO.
     *
     * <p>A primeira versao da definicao contava os caminhos minimos dentro do laco de
     * Floyd e Warshall e contava duas vezes. Nenhuma forma desenhada a mao pegou isso,
     * e a coluna do meio diz por que: no caminho, na estrela, no haltere e no completo
     * ha um caminho minimo so entre cada par, e contar certo e contar errado dao
     * exatamente a mesma coisa.
     *
     * <p>Nos grafos sorteados ha de nove a dezessete pares com mais de um caminho, e ai
     * a diferenca aparece.
     */
    @Test
    @DisplayName("as formas desenhadas a mao tem zero pares com mais de um caminho minimo, e os sorteados tem nove ou mais")
    void aTabelaDasTresContas() {
        List<Medidas.LinhaTresContas> linhas = Medidas.asTresContas();

        assertThat(linhas).hasSize(9);

        // as formas que NAO podiam pegar o erro
        for (String nome : new String[] {"caminho de 8", "estrela de 7", "haltere de 3", "completo de 5"})
            assertThat(acharTresContas(linhas, nome).paresComMaisDeUmCaminho()).as(nome).isZero();

        // e as que podiam
        assertThat(acharTresContas(linhas, "ciclo de 8").paresComMaisDeUmCaminho()).isEqualTo(4);
        assertThat(acharTresContas(linhas, "dirigido fechado de 8").paresComMaisDeUmCaminho()).isEqualTo(24);

        for (long semente : new long[] {1, 2, 3})
            assertThat(acharTresContas(linhas, "sorteado de 8 a 0.3 (" + semente + ")")
                    .paresComMaisDeUmCaminho()).as("semente %d", semente).isGreaterThanOrEqualTo(9);

        // e hoje as tres contas concordam em todas as linhas
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.desvioDeBrandes()).as("Brandes em %s", l.grafo()).isLessThan(1e-12);
            assertThat(l.desvioDaDefinicao()).as("a definicao em %s", l.grafo()).isLessThan(1e-12);
        });
    }

    // ───────────────────── o vazamento ─────────────────────

    /**
     * O ACHADO DO VAZAMENTO: num grafo dirigido de QUATRO vertices com um beco, a
     * conta ingenua do PageRank perde 48% da probabilidade e nao reclama.
     *
     * <p>E o buraco diminui com o tamanho: num beco de vinte ele vaza 7%.
     */
    @Test
    @DisplayName("o beco de quatro vaza 48% e o de vinte vaza 7%, e com tratamento a soma e sempre um")
    void aTabelaDoVazamento() {
        List<Medidas.LinhaVazamento> linhas = Medidas.oVazamento();

        assertThat(linhas).hasSize(7);

        assertThat(acharVazamento(linhas, "com beco de 4").vazou()).isCloseTo(0.4777, within(5e-4));
        assertThat(acharVazamento(linhas, "com beco de 6").vazou()).isCloseTo(0.2773, within(5e-4));
        assertThat(acharVazamento(linhas, "com beco de 10").vazou()).isCloseTo(0.1491, within(5e-4));
        assertThat(acharVazamento(linhas, "com beco de 20").vazou()).isCloseTo(0.0739, within(5e-4));

        // e o vazamento cai monotonamente com o tamanho
        assertThat(acharVazamento(linhas, "com beco de 4").vazou())
                .isGreaterThan(acharVazamento(linhas, "com beco de 6").vazou());
        assertThat(acharVazamento(linhas, "com beco de 6").vazou())
                .isGreaterThan(acharVazamento(linhas, "com beco de 10").vazou());
        assertThat(acharVazamento(linhas, "com beco de 10").vazou())
                .isGreaterThan(acharVazamento(linhas, "com beco de 20").vazou());

        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.somaComTratamento()).as("soma com tratamento em %s", l.grafo())
                    .isCloseTo(1.0, within(1e-9));
            assertThat(l.desvioDoSistema()).as("contra o sistema em %s", l.grafo()).isLessThan(1e-12);
        });
    }

    @Test
    @DisplayName("grafo sem beco nao vaza nada, e ai as duas versoes sao a mesma coisa")
    void oGrafoSemBecoNaoVaza() {
        List<Medidas.LinhaVazamento> linhas = Medidas.oVazamento();

        for (String nome : new String[] {"dirigido fechado de 6", "caminho de 5", "estrela de 7"}) {
            Medidas.LinhaVazamento l = acharVazamento(linhas, nome);

            assertThat(l.semSaida()).as(nome).isZero();
            assertThat(l.vazou()).as(nome).isCloseTo(0.0, within(1e-9));
            assertThat(l.somaSemTratamento()).as(nome).isCloseTo(l.somaComTratamento(), within(1e-9));
        }

        for (String nome : new String[] {"com beco de 4", "com beco de 6", "com beco de 10", "com beco de 20"})
            assertThat(acharVazamento(linhas, nome).semSaida()).as(nome).isEqualTo(1);
    }

    /**
     * A CONVERGENCIA so da para medir porque ha um juiz EXATO.
     *
     * <p>Sem o sistema linear, o criterio de parada seria "parou de mudar", que e outra
     * coisa. A tabela mostra que vinte rodadas ja erram na oitava casa e cinquenta
     * chegam no limite do ponto flutuante.
     */
    @Test
    @DisplayName("a iteracao chega no limite do ponto flutuante em cinquenta rodadas")
    void aTabelaDaConvergencia() {
        List<Medidas.LinhaConvergencia> linhas = Medidas.aConvergencia();

        assertThat(linhas).hasSize(8);

        // o desvio so cai, nunca sobe
        for (int i = 1; i < linhas.size(); i++)
            assertThat(linhas.get(i).desvioDoSistema())
                    .as("de %d para %d rodadas", linhas.get(i - 1).rodadas(), linhas.get(i).rodadas())
                    .isLessThanOrEqualTo(linhas.get(i - 1).desvioDoSistema());

        assertThat(acharConvergencia(linhas, 1).desvioDoSistema()).isGreaterThan(1e-3);
        assertThat(acharConvergencia(linhas, 20).desvioDoSistema()).isBetween(1e-9, 1e-7);
        assertThat(acharConvergencia(linhas, 50).desvioDoSistema()).isLessThan(1e-14);

        // e a soma e um em toda rodada, inclusive na primeira
        assertThat(linhas).allSatisfy(l ->
                assertThat(l.soma()).as("%d rodadas", l.rodadas()).isCloseTo(1.0, within(1e-9)));
    }

    // ───────────────────── o custo ─────────────────────

    /**
     * BRANDES contra a DEFINICAO, em visitas.
     *
     * <p>A definicao custa o cubo do numero de vertices e nao olha para as arestas;
     * Brandes custa vertices vezes arestas. Em grafo esparso a diferenca CRESCE com o
     * tamanho, e e isso que a ultima coluna mede.
     */
    @Test
    @DisplayName("em grafo esparso a vantagem de Brandes cresce com o tamanho, de 3 para 19 vezes")
    void aTabelaDoCusto() {
        List<Medidas.LinhaCusto> linhas = Medidas.oCusto();

        assertThat(linhas).hasSize(4);
        assertThat(linhas).extracting(Medidas.LinhaCusto::vertices).containsExactly(10, 20, 40, 60);

        for (int i = 1; i < linhas.size(); i++)
            assertThat(linhas.get(i).quantasVezesMenos())
                    .as("de %d para %d vertices", linhas.get(i - 1).vertices(), linhas.get(i).vertices())
                    .isGreaterThan(linhas.get(i - 1).quantasVezesMenos());

        assertThat(acharCusto(linhas, 10).quantasVezesMenos()).isBetween(3.0, 4.0);
        assertThat(acharCusto(linhas, 60).quantasVezesMenos()).isBetween(18.0, 21.0);

        assertThat(linhas).allSatisfy(l ->
                assertThat(l.porBrandes()).as("%d vertices", l.vertices()).isLessThan(l.pelaDefinicao()));
    }

    /**
     * E o grafo da tabela e ESPARSO de proposito: a chance e dois sobre n, que deixa o
     * numero de arestas crescendo EM LINHA e nao no quadrado.
     *
     * <p>Sem isso a tabela nao mediria nada: em grafo denso as arestas chegam perto do
     * quadrado dos vertices e as duas contas custam a mesma coisa.
     */
    @Test
    @DisplayName("o grafo da tabela de custo e esparso, com arestas crescendo em linha")
    void oGrafoDoCustoEEsparso() {
        List<Medidas.LinhaCusto> linhas = Medidas.oCusto();

        for (Medidas.LinhaCusto l : linhas) {
            // arestas por vertice fica contido, em vez de crescer com n
            assertThat((double) l.arestas() / l.vertices()).as("%d vertices", l.vertices())
                    .isBetween(1.0, 3.0);

            // e fica bem abaixo das arestas de um grafo completo do mesmo tamanho
            assertThat(l.arestas()).as("%d vertices", l.vertices())
                    .isLessThan(l.vertices() * (l.vertices() - 1) / 4);
        }
    }

    // ───────────────────── o centro ─────────────────────

    /**
     * O ACHADO PRINCIPAL DO REPOSITORIO: as quatro medidas DISCORDAM sobre quem e o
     * centro, e discordam na maioria das formas desenhadas a mao.
     *
     * <p>Isso nao e defeito de nenhuma delas: sao perguntas diferentes.
     */
    @Test
    @DisplayName("as quatro medidas discordam sobre o centro em cinco dos onze grafos")
    void aTabelaDoCentro() {
        List<Medidas.LinhaCentro> linhas = Medidas.oCentro();

        assertThat(linhas).hasSize(11);

        long discordam = linhas.stream().filter(l -> !l.asQuatroConcordam()).count();

        assertThat(discordam).isEqualTo(5);

        assertThat(linhas.stream().filter(l -> !l.asQuatroConcordam()).map(Medidas.LinhaCentro::grafo))
                .containsExactlyInAnyOrder("caminho de 5", "caminho de 9", "haltere de 4",
                        "haltere de 6", "com beco de 6");

        // no caminho, o grau aponta para um vertice qualquer e as outras tres para o meio
        Medidas.LinhaCentro caminho = acharCentro(linhas, "caminho de 9");

        assertThat(caminho.porGrau()).isEqualTo(1);
        assertThat(caminho.porProximidade()).isEqualTo(4);
        assertThat(caminho.porIntermediacao()).isEqualTo(4);
        assertThat(caminho.porPageRank()).isEqualTo(1);
        assertThat(caminho.asQuatroConcordam()).isFalse();

        // na estrela, no ciclo e no completo elas concordam, e por isso essas formas
        // nao separam nada: estao aqui como controle
        for (String nome : new String[] {"estrela de 7", "ciclo de 6", "completo de 5"})
            assertThat(acharCentro(linhas, nome).asQuatroConcordam()).as(nome).isTrue();
    }

    /**
     * No HALTERE a discordancia e a mais limpa: grau e PageRank apontam para a roda, e
     * proximidade e intermediacao apontam para a ponte.
     */
    @Test
    @DisplayName("no haltere o grau e o PageRank apontam para a roda e as outras duas para a ponte")
    void noHaltereAsMedidasSeDividemEmDuas() {
        List<Medidas.LinhaCentro> linhas = Medidas.oCentro();

        for (int porLado : new int[] {4, 6}) {
            Medidas.LinhaCentro l = acharCentro(linhas, "haltere de " + porLado);

            assertThat(l.porIntermediacao()).as("intermediacao no haltere de %d", porLado).isEqualTo(porLado);
            assertThat(l.porProximidade()).as("proximidade no haltere de %d", porLado).isEqualTo(porLado);
            assertThat(l.porGrau()).as("grau no haltere de %d", porLado).isNotEqualTo(porLado);
            assertThat(l.porPageRank()).as("PageRank no haltere de %d", porLado).isNotEqualTo(porLado);
            assertThat(l.asQuatroConcordam()).isFalse();
        }
    }

    /**
     * O NUMERO QUE DA O TITULO DO REPOSITORIO: a ponte tem o MENOR grau e a MAIOR
     * intermediacao. Quem conhece menos gente e exatamente quem nao pode faltar.
     *
     * <p>E, do outro lado, a medida que me corrigiu: com TRES por lado isso nao vale.
     * A roda tem vertices de grau dois tambem, a ponte empata e cai para o quinto
     * lugar de sete. A inversao limpa so comeca em quatro por lado.
     */
    @Test
    @DisplayName("a ponte e a primeira em intermediacao sempre, e a ultima em grau so a partir de quatro por lado")
    void aTabelaDoHaltere() {
        List<Medidas.LinhaHaltere> linhas = Medidas.oHaltere();

        assertThat(linhas).hasSize(4);
        assertThat(linhas).extracting(Medidas.LinhaHaltere::porLado).containsExactly(3, 4, 6, 8);

        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.ponte()).isEqualTo(l.porLado());
            assertThat(l.vertices()).isEqualTo(2 * l.porLado() + 1);
            assertThat(l.postoPorIntermediacao()).as("posto por intermediacao em %d", l.porLado()).isEqualTo(1);
        });

        // o empate de tres por lado
        assertThat(acharHaltere(linhas, 3).postoPorGrau()).isEqualTo(5);

        // e a inversao limpa a partir de quatro
        for (int porLado : new int[] {4, 6, 8})
            assertThat(acharHaltere(linhas, porLado).postoPorGrau()).as("posto por grau em %d", porLado)
                    .isEqualTo(2 * porLado + 1);
    }

    /**
     * E o aperto: quanto MAIOR a roda, MENOR o grau da ponte e MAIOR a distancia entre
     * as duas medidas.
     *
     * <p>A intermediacao da ponte cai um pouco com o tamanho, porque os pares dentro de
     * cada roda crescem no quadrado e nao passam por ela. Mesmo caindo, ela segue em
     * primeiro.
     */
    @Test
    @DisplayName("quanto maior a roda, menor o grau da ponte, e a intermediacao fica acima de meio")
    void oHaltereApertaComOTamanho() {
        List<Medidas.LinhaHaltere> linhas = Medidas.oHaltere();

        for (int i = 1; i < linhas.size(); i++) {
            assertThat(linhas.get(i).grauDaPonte()).as("grau da ponte em %d", linhas.get(i).porLado())
                    .isLessThan(linhas.get(i - 1).grauDaPonte());

            assertThat(linhas.get(i).intermediacaoDaPonte())
                    .as("intermediacao da ponte em %d", linhas.get(i).porLado())
                    .isLessThan(linhas.get(i - 1).intermediacaoDaPonte());
        }

        assertThat(acharHaltere(linhas, 8).grauDaPonte()).isCloseTo(0.125, within(1e-9));
        assertThat(acharHaltere(linhas, 8).intermediacaoDaPonte()).isCloseTo(0.5333, within(5e-4));

        assertThat(linhas).allSatisfy(l ->
                assertThat(l.intermediacaoDaPonte()).as("em %d por lado", l.porLado()).isGreaterThan(0.5));
    }

    // ───────────────────── achadores ─────────────────────

    private static Medidas.LinhaTresContas acharTresContas(List<Medidas.LinhaTresContas> linhas, String grafo) {
        return linhas.stream().filter(l -> l.grafo().equals(grafo)).findFirst().orElseThrow();
    }

    private static Medidas.LinhaConhecida acharConhecida(List<Medidas.LinhaConhecida> linhas, String grafo, String medida) {
        return linhas.stream()
                .filter(l -> l.grafo().equals(grafo) && l.medida().equals(medida))
                .findFirst()
                .orElseThrow();
    }

    private static Medidas.LinhaVazamento acharVazamento(List<Medidas.LinhaVazamento> linhas, String grafo) {
        return linhas.stream().filter(l -> l.grafo().equals(grafo)).findFirst().orElseThrow();
    }

    private static Medidas.LinhaConvergencia acharConvergencia(List<Medidas.LinhaConvergencia> linhas, int rodadas) {
        return linhas.stream().filter(l -> l.rodadas() == rodadas).findFirst().orElseThrow();
    }

    private static Medidas.LinhaCusto acharCusto(List<Medidas.LinhaCusto> linhas, int vertices) {
        return linhas.stream().filter(l -> l.vertices() == vertices).findFirst().orElseThrow();
    }

    private static Medidas.LinhaCentro acharCentro(List<Medidas.LinhaCentro> linhas, String grafo) {
        return linhas.stream().filter(l -> l.grafo().equals(grafo)).findFirst().orElseThrow();
    }

    private static Medidas.LinhaHaltere acharHaltere(List<Medidas.LinhaHaltere> linhas, int porLado) {
        return linhas.stream().filter(l -> l.porLado() == porLado).findFirst().orElseThrow();
    }
}
