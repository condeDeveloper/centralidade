package br.com.conde.centralidade;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * As MEDIDAS DE CENTRALIDADE, e a conta de cada uma.
 *
 * <p>Esta classe tem as implementacoes RAPIDAS. As lentas, que servem de juiz, ficam
 * no {@link Juiz}, e as duas tem de dar o mesmo numero em todo grafo.
 *
 * <p>O contador existe porque o assunto da metade das tabelas e o CUSTO: a
 * intermediacao pela definicao custa o cubo do numero de vertices, e por Brandes
 * custa vertices vezes arestas. Em grafo esparso isso e a diferenca entre cabivel e
 * nao cabivel.
 */
public final class Centralidades {

    private Centralidades() {
    }

    /**
     * O GRAU: quantos vizinhos o vertice tem.
     *
     * <p>A medida mais simples que existe e, em muitos grafos, a que mais engana. A
     * tabela do haltere mostra: o vertice da ponte tem o MENOR grau e a MAIOR
     * intermediacao.
     *
     * <p>Normalizado por n menos um, para dar um numero entre zero e um que da para
     * comparar entre grafos de tamanhos diferentes.
     */
    public static double[] grau(Grafo g) {
        double[] saida = new double[g.vertices()];

        for (int v = 0; v < g.vertices(); v++)
            saida[v] = g.vertices() <= 1 ? 0 : (double) g.saindoDe(v).size() / (g.vertices() - 1);

        return saida;
    }

    /**
     * As DISTANCIAS de um vertice para todos, por busca em largura.
     *
     * <p>Devolve menos um para os inalcancaveis. Grafo com vertice inalcancavel nao e
     * caso de borda: e o caso comum em grafo dirigido.
     */
    public static int[] distanciasDe(Grafo g, int origem, Contador contador) {
        int[] distancia = new int[g.vertices()];

        Arrays.fill(distancia, -1);
        distancia[origem] = 0;

        Deque<Integer> fila = new ArrayDeque<>();

        fila.add(origem);

        while (!fila.isEmpty()) {
            int u = fila.poll();

            contador.visitou();

            for (int v : g.saindoDe(u)) {
                contador.visitou();

                if (distancia[v] < 0) {
                    distancia[v] = distancia[u] + 1;
                    fila.add(v);
                }
            }
        }

        return distancia;
    }

    /**
     * A PROXIMIDADE: o inverso da soma das distancias ate todos os alcancaveis.
     *
     * <p>Normalizada pelo numero de alcancaveis, que e o que torna a medida comparavel
     * num grafo desconexo. Sem essa normalizacao, um vertice isolado num canto de um
     * grafo grande ganharia a medida por alcancar pouca gente de perto.
     */
    public static double[] proximidade(Grafo g, Contador contador) {
        double[] saida = new double[g.vertices()];

        for (int origem = 0; origem < g.vertices(); origem++) {
            int[] distancia = distanciasDe(g, origem, contador);

            long soma = 0;
            int alcancaveis = 0;

            for (int v = 0; v < g.vertices(); v++)
                if (v != origem && distancia[v] > 0) {
                    soma += distancia[v];
                    alcancaveis++;
                }

            if (soma == 0 || alcancaveis == 0) {
                saida[origem] = 0;
                continue;
            }

            // a normalizacao de Wasserman e Faust: vale em grafo desconexo
            double media = (double) alcancaveis / soma;

            saida[origem] = g.vertices() <= 1 ? 0 : media * alcancaveis / (g.vertices() - 1);
        }

        return saida;
    }

    /**
     * A INTERMEDIACAO por BRANDES, de 2001.
     *
     * <p>A definicao pede, para cada par de vertices, quantos caminhos minimos passam
     * por cada terceiro vertice. Escrita assim, ela custa o cubo do numero de
     * vertices e exige guardar todos os caminhos.
     *
     * <p>Brandes percebeu que da para acumular isso de tras para a frente em cada
     * busca em largura: uma passada para a frente conta os caminhos minimos, e uma
     * passada para tras distribui o credito. O custo cai para vertices vezes arestas.
     *
     * <p>A pilha de volta e explicita, e nao recursiva: a profundidade da arvore de
     * busca pode ser o numero de vertices, e um caminho de cem mil vertices estoura a
     * pilha da maquina virtual em qualquer sistema.
     */
    public static double[] intermediacao(Grafo g, Contador contador) {
        int n = g.vertices();
        double[] saida = new double[n];

        for (int origem = 0; origem < n; origem++) {
            Deque<Integer> ordem = new ArrayDeque<>();
            List<List<Integer>> anteriores = new ArrayList<>(n);
            double[] caminhos = new double[n];
            int[] distancia = new int[n];

            for (int i = 0; i < n; i++) anteriores.add(new ArrayList<>());

            Arrays.fill(distancia, -1);

            caminhos[origem] = 1;
            distancia[origem] = 0;

            Deque<Integer> fila = new ArrayDeque<>();

            fila.add(origem);

            while (!fila.isEmpty()) {
                int u = fila.poll();

                ordem.push(u);
                contador.visitou();

                for (int v : g.saindoDe(u)) {
                    contador.visitou();

                    if (distancia[v] < 0) {
                        distancia[v] = distancia[u] + 1;
                        fila.add(v);
                    }

                    if (distancia[v] == distancia[u] + 1) {
                        caminhos[v] += caminhos[u];
                        anteriores.get(v).add(u);
                    }
                }
            }

            double[] credito = new double[n];

            while (!ordem.isEmpty()) {
                int w = ordem.pop();

                for (int u : anteriores.get(w)) {
                    contador.visitou();
                    credito[u] += caminhos[u] / caminhos[w] * (1 + credito[w]);
                }

                if (w != origem) saida[w] += credito[w];
            }
        }

        // num grafo nao dirigido cada par e contado duas vezes
        if (!g.dirigido()) for (int v = 0; v < n; v++) saida[v] /= 2;

        return normalizarIntermediacao(saida, n, g.dirigido());
    }

    static double[] normalizarIntermediacao(double[] bruto, int n, boolean dirigido) {
        if (n <= 2) return new double[n];

        double fator = dirigido ? (n - 1.0) * (n - 2) : (n - 1.0) * (n - 2) / 2;
        double[] saida = new double[n];

        for (int v = 0; v < n; v++) saida[v] = bruto[v] / fator;

        return saida;
    }

    /**
     * O PAGERANK por ITERACAO DE POTENCIA, com o tratamento dos vertices SEM SAIDA.
     *
     * <p>A massa que chega num vertice sem saida nao tem para onde ir. Esta versao
     * redistribui essa massa igualmente entre todos, que e o mesmo que dizer que o
     * passeio aleatorio, ao chegar num beco, pula para um vertice qualquer.
     *
     * <p>A versao ingenua simplesmente perde essa massa, e a soma das notas deixa de
     * ser um sem ninguem avisar. A tabela do vazamento mede isso.
     *
     * @param amortecimento a chance de seguir uma aresta em vez de pular
     */
    public static double[] pageRank(Grafo g, double amortecimento, int rodadas, boolean tratarSemSaida) {
        int n = g.vertices();

        if (n == 0) return new double[0];

        double[] nota = new double[n];

        Arrays.fill(nota, 1.0 / n);

        List<Integer> becos = g.semSaida();

        for (int rodada = 0; rodada < rodadas; rodada++) {
            double[] nova = new double[n];
            double vazada = 0;

            for (int beco : becos) vazada += nota[beco];

            for (int v = 0; v < n; v++) {
                double entrando = 0;

                for (int u : g.chegandoEm(v)) entrando += nota[u] / g.grauDeSaida(u);

                nova[v] = (1 - amortecimento) / n + amortecimento * entrando;

                if (tratarSemSaida) nova[v] += amortecimento * vazada / n;
            }

            nota = nova;
        }

        return nota;
    }

    /** A soma das notas, que tem de ser um numa conta certa. */
    public static double soma(double[] notas) {
        double total = 0;

        for (double x : notas) total += x;

        return total;
    }

    public static int maior(double[] valores) {
        int escolhido = 0;

        for (int i = 1; i < valores.length; i++) if (valores[i] > valores[escolhido]) escolhido = i;

        return escolhido;
    }
}
