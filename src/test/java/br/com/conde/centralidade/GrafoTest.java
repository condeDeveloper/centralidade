package br.com.conde.centralidade;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Os testes do GRAFO e das formas que as medidas usam. */
class GrafoTest {

    @Test
    @DisplayName("numa aresta nao dirigida os dois lados se enxergam")
    void aArestaNaoDirigidaValeNosDoisSentidos() {
        Grafo g = Grafo.com("dois", 2, false).aresta(0, 1).pronto();

        assertThat(g.saindoDe(0)).containsExactly(1);
        assertThat(g.saindoDe(1)).containsExactly(0);
        assertThat(g.chegandoEm(0)).containsExactly(1);
        assertThat(g.chegandoEm(1)).containsExactly(0);
        assertThat(g.dirigido()).isFalse();
    }

    @Test
    @DisplayName("numa aresta dirigida so um lado enxerga o outro")
    void aArestaDirigidaValeNumSentidoSo() {
        Grafo g = Grafo.com("dois", 2, true).aresta(0, 1).pronto();

        assertThat(g.saindoDe(0)).containsExactly(1);
        assertThat(g.saindoDe(1)).isEmpty();
        assertThat(g.chegandoEm(1)).containsExactly(0);
        assertThat(g.semSaida()).containsExactly(1);
    }

    @Test
    @DisplayName("a aresta recusa vertice fora da faixa")
    void aArestaRecusaVerticeForaDaFaixa() {
        assertThatThrownBy(() -> Grafo.com("tres", 3, false).aresta(0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora de 0 a 2");

        assertThatThrownBy(() -> Grafo.com("tres", 3, false).aresta(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("as listas do grafo pronto nao aceitam mudanca")
    void oGrafoProntoNaoMuda() {
        Grafo g = Grafo.caminho(4);

        assertThatThrownBy(() -> g.saindoDe(0).add(3))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("o caminho tem n menos uma arestas e as pontas tem grau um")
    void oCaminho() {
        Grafo g = Grafo.caminho(5);

        assertThat(g.vertices()).isEqualTo(5);
        assertThat(g.arestas()).isEqualTo(4);
        assertThat(g.grauDeSaida(0)).isEqualTo(1);
        assertThat(g.grauDeSaida(4)).isEqualTo(1);
        assertThat(g.grauDeSaida(2)).isEqualTo(2);
        assertThat(g.semSaida()).isEmpty();
    }

    @Test
    @DisplayName("a estrela tem um centro ligado a todos e pontas de grau um")
    void aEstrela() {
        Grafo g = Grafo.estrela(7);

        assertThat(g.arestas()).isEqualTo(6);
        assertThat(g.grauDeSaida(0)).isEqualTo(6);

        for (int v = 1; v < 7; v++) assertThat(g.grauDeSaida(v)).as("ponta %d", v).isEqualTo(1);
    }

    @Test
    @DisplayName("no ciclo todos tem grau dois")
    void oCiclo() {
        Grafo g = Grafo.ciclo(6);

        assertThat(g.arestas()).isEqualTo(6);

        for (int v = 0; v < 6; v++) assertThat(g.grauDeSaida(v)).as("vertice %d", v).isEqualTo(2);
    }

    @Test
    @DisplayName("no completo todos tem grau n menos um")
    void oCompleto() {
        Grafo g = Grafo.completo(5);

        assertThat(g.arestas()).isEqualTo(10);

        for (int v = 0; v < 5; v++) assertThat(g.grauDeSaida(v)).as("vertice %d", v).isEqualTo(4);
    }

    /**
     * O HALTERE e a forma que separa grau de intermediacao: a ponte tem grau dois,
     * que e o MENOR do grafo.
     *
     * <p>Menor, e nao estritamente menor. Com TRES por lado, um vertice da roda
     * tambem tem grau dois, e a ponte EMPATA. A separacao so fica limpa a partir de
     * quatro por lado, e essa medida esta na tabela do haltere.
     */
    @Test
    @DisplayName("a ponte do haltere tem grau dois, que empata em tres por lado e ganha a partir de quatro")
    void oHaltereTemAPonteDeGrauMinimo() {
        for (int porLado : new int[] {3, 4, 6, 8}) {
            Grafo g = Grafo.haltere(porLado);
            int ponte = porLado;

            assertThat(g.vertices()).isEqualTo(2 * porLado + 1);
            assertThat(g.grauDeSaida(ponte)).as("ponte de %d", porLado).isEqualTo(2);

            for (int v = 0; v < g.vertices(); v++)
                if (v != ponte)
                    assertThat(g.grauDeSaida(v)).as("vertice %d do haltere de %d", v, porLado)
                            .isGreaterThanOrEqualTo(2);
        }

        // com tres por lado os vertices 0 e 1 da roda tambem tem grau dois
        assertThat(Grafo.haltere(3).grauDeSaida(0)).isEqualTo(2);

        // a partir de quatro por lado a ponte e a unica de grau dois
        for (int porLado : new int[] {4, 6, 8}) {
            Grafo g = Grafo.haltere(porLado);

            for (int v = 0; v < g.vertices(); v++)
                if (v != porLado)
                    assertThat(g.grauDeSaida(v)).as("vertice %d do haltere de %d", v, porLado)
                            .isGreaterThan(2);
        }
    }

    @Test
    @DisplayName("o grafo com beco tem exatamente um vertice sem saida, e e o ultimo")
    void oBeco() {
        for (int n : new int[] {4, 6, 10, 20}) {
            Grafo g = Grafo.comBeco(n);

            assertThat(g.dirigido()).isTrue();
            assertThat(g.semSaida()).as("beco de %d", n).containsExactly(n - 1);
        }
    }

    @Test
    @DisplayName("o dirigido fechado nao tem beco nenhum")
    void oDirigidoFechadoNaoVaza() {
        Grafo g = Grafo.dirigidoFechado(6);

        assertThat(g.dirigido()).isTrue();
        assertThat(g.semSaida()).isEmpty();
    }

    /**
     * O grafo sorteado tem uma arvore de base de proposito: sem ela, uma chance baixa
     * deixaria vertices soltos e a tabela mediria desconexao em vez de centralidade.
     */
    @Test
    @DisplayName("o grafo sorteado e sempre conexo, mesmo com chance baixa")
    void oSorteadoEConexo() {
        Contador contador = new Contador();

        for (long semente = 1; semente <= 30; semente++) {
            Grafo g = Grafo.sorteado(25, 0.02, semente);
            int[] distancia = Centralidades.distanciasDe(g, 0, contador);

            for (int v = 0; v < 25; v++)
                assertThat(distancia[v]).as("semente %d, vertice %d", semente, v)
                        .isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("a lista todos() traz onze grafos com nomes distintos")
    void aListaDeTodos() {
        Map<String, Grafo> todos = Grafo.todos();

        assertThat(todos).hasSize(11);
        assertThat(todos.keySet()).doesNotHaveDuplicates();
        assertThat(todos).allSatisfy((nome, g) -> assertThat(g.nome()).isEqualTo(nome));
    }

    @Test
    @DisplayName("o toString diz o tamanho e se e dirigido")
    void oToString() {
        assertThat(Grafo.caminho(5).toString()).isEqualTo("caminho de 5 (5 vertices, 4 arestas, nao dirigido)");
        assertThat(Grafo.comBeco(4).toString()).contains("dirigido").doesNotContain("nao dirigido");
    }

    // ───────────────────────────── o sorteio ─────────────────────────────

    @Test
    @DisplayName("a mesma semente da a mesma sequencia, e sementes diferentes dao sequencias diferentes")
    void oSorteioERepetivel() {
        Grafo.Sorteio um = new Grafo.Sorteio(7);
        Grafo.Sorteio outro = new Grafo.Sorteio(7);

        for (int i = 0; i < 200; i++) assertThat(um.proximo()).isEqualTo(outro.proximo());

        assertThat(new Grafo.Sorteio(7).proximo()).isNotEqualTo(new Grafo.Sorteio(8).proximo());
    }

    @Test
    @DisplayName("a semente zero nao trava o gerador num valor so")
    void aSementeZeroNaoTrava() {
        Grafo.Sorteio sorteio = new Grafo.Sorteio(0);
        Set<Integer> vistos = new HashSet<>();

        for (int i = 0; i < 100; i++) vistos.add(sorteio.proximo());

        assertThat(vistos).hasSizeGreaterThan(90);
    }

    /**
     * O DESLOCAMENTO DE 32 BITS nao e enfeite.
     *
     * <p>Num congruente linear, o bit mais baixo do estado alterna par e impar a cada
     * passo, com periodo dois. Se o sorteio devolvesse o estado cru, as arestas
     * sairiam num xadrez e o "grafo sorteado" seria uma forma fixa disfarcada.
     *
     * <p>O teste mede os dois: o estado cru alterna sempre, e o deslocado nao.
     */
    @Test
    @DisplayName("o deslocamento de 32 bits tira a alternancia de par e impar dos bits baixos")
    void oDeslocamentoTiraAAlternancia() {
        long estado = 12345;
        int alternouNoCru = 0;
        int anteriorCru = (int) (estado & 1);

        for (int i = 0; i < 1000; i++) {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            int agora = (int) (estado & 1);

            if (agora != anteriorCru) alternouNoCru++;

            anteriorCru = agora;
        }

        // o bit mais baixo do estado cru alterna em TODO passo
        assertThat(alternouNoCru).isEqualTo(1000);

        Grafo.Sorteio sorteio = new Grafo.Sorteio(12345);
        int alternouNoDeslocado = 0;
        int anterior = sorteio.proximo() & 1;

        for (int i = 0; i < 1000; i++) {
            int agora = sorteio.proximo() & 1;

            if (agora != anterior) alternouNoDeslocado++;

            anterior = agora;
        }

        // e depois do deslocamento ele alterna perto de metade das vezes, como moeda
        assertThat(alternouNoDeslocado).isBetween(420, 580);
    }

    @Test
    @DisplayName("a fracao fica entre zero e um e o ate() fica dentro do limite")
    void aFracaoEOAte() {
        Grafo.Sorteio sorteio = new Grafo.Sorteio(99);

        for (int i = 0; i < 2000; i++) {
            assertThat(sorteio.fracao()).isBetween(0.0, 1.0);
            assertThat(sorteio.ate(7)).isBetween(0, 6);
        }
    }

    /**
     * A fracao tem de cobrir o intervalo inteiro: um gerador que so devolvesse numeros
     * na metade de baixo passaria no teste de faixa e estragaria todo grafo sorteado.
     */
    @Test
    @DisplayName("a fracao cobre as dez faixas do intervalo")
    void aFracaoCobreOIntervalo() {
        Grafo.Sorteio sorteio = new Grafo.Sorteio(4321);
        int[] caixas = new int[10];

        for (int i = 0; i < 10000; i++) caixas[(int) (sorteio.fracao() * 10)]++;

        for (int i = 0; i < 10; i++) assertThat(caixas[i]).as("faixa %d", i).isBetween(850, 1150);
    }
}
