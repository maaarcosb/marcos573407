import java.util.Scanner;
import java.util.Random;

public class CidadeChuva {
    static Scanner sc = new Scanner(System.in);
    static Random rm;


    // Soma todos os valores da matriz de água.
    // A gente usa isso para conferir se a água total continua igual depois dos ciclos, já que ela só escoa de um lado para o outro.
    public static int somar(int[][] matriz) {
        int totalAgua = 0;
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                totalAgua = totalAgua + matriz[i][j];
            }
        }
        return totalAgua;
    }

    // Dá uma geral na matriz booleana de alerta para contar quantas células ficaram alagadas no total.
    public static int contarAlerta(boolean[][] alerta) {
        int qtd = 0;
        for (int i = 0; i < alerta.length; i++) {
            for (int j = 0; j < alerta[0].length; j++) {
                if (alerta[i][j] == true) {
                    qtd++;
                }
            }
        }
        return qtd;
    }

    public static void main(String[] args) {
        // Fica perguntando até o usuário digitar uma quantidade válida de integrantes (de 1 a 3)
        int tam = 0;
        while (tam < 1 || tam > 3) {
            System.out.println("Por favor, informe a quantidade de integrantes (1 a 3):");
            tam = sc.nextInt();
        }

        int[] listaRms = new int[tam];
        for (int i = 0; i < tam; i++) {
            System.out.println("Informe o RM do integrante " + (i + 1) + ":");
            listaRms[i] = sc.nextInt();
        }

        // Calcula a semente da equipe e puxa os parâmetros do bairro
        int rmEquipe = calcularRmEquipe(listaRms);
        int[] params = calcularParametros(rmEquipe);

        int n = params[0];
        int m = params[1];
        int k = params[2];
        int l = params[3];
        int ordem = params[4];

        // Inicia o gerador com o RM da equipe e monta o terreno e a chuva inicial
        rm = new Random(rmEquipe);
        int[][] alt = gerarAltitude(rm, n, m);
        int[][] lamInicial = gerarMatriz(rm, n, m, 0, 10);

        System.out.println("RM da equipe: " + rmEquipe);
        System.out.println("Parametros -> N=" + n + " M=" + m + " K=" + k + " L=" + l + " ordem=" + ordem);
        imprimir(alt, "Altitude (cm):");
        imprimir(lamInicial, "Lamina inicial (cm):");

        int[] dLin = criarDeslocamentoLinha(ordem);
        int[] dCol = criarDeslocamentoColuna(ordem);

        int totalIni = somar(lamInicial);
        int[][] lamAtual = copiar(lamInicial);
        boolean[][] jaAlertou = new boolean[n][m];

        // Guarda o estado no passo 2 para comparar lá na frente nos itens D1 e D2
        int passoD = 2;
        int[][] guardadaD1 = null;

        System.out.println("Simulacao (" + k + " ciclos):");
        for (int p = 1; p <= k; p++) {
            int antes = somar(lamAtual);
            lamAtual = simularPasso(alt, lamAtual, dLin, dCol);
            int depois = somar(lamAtual);

            System.out.println("Ciclo " + p + ": agua antes = " + antes + " cm, agua depois = " + depois + " cm");
            if (p == 1) {
                imprimir(lamAtual, "Lamina apos o 1o ciclo (cm):");
            }

            // Checa se alguma célula atingiu o limite de alerta pela primeira vez neste ciclo
            boolean[][] alertaCiclo = marcarAlerta(lamAtual, l);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (alertaCiclo[i][j] == true && jaAlertou[i][j] == false) {
                        jaAlertou[i][j] = true;
                        System.out.println("ALERTA: ciclo " + p + ", celula (" + i + "," + j + ") chegou a "
                                + lamAtual[i][j] + " cm (limite " + l + " cm)");
                    }
                }
            }

            if (p == passoD) {
                guardadaD1 = copiar(lamAtual);
            }
        }

        int totalFim = somar(lamAtual);
        imprimir(lamAtual, "Lamina apos " + k + " passos (cm):");
        System.out.println("Total de agua no inicio: " + totalIni + " cm");
        System.out.println("Total de agua no fim: " + totalFim + " cm");

        boolean[][] alerta = marcarAlerta(lamAtual, l);
        int numAlertas = contarAlerta(alerta);
        System.out.println("Celulas em alerta: " + numAlertas);

        imprimirMapaAlerta(alerta);

        int[] porLinha = calcularAguaPorLinha(lamAtual);
        System.out.println("Água por linha (cm):");
        for (int i = 0; i < n; i++) {
            System.out.println("Linha " + i + ": " + porLinha[i]);
        }

        int idxCritica = encontrarLinhaCritica(porLinha);
        int qtdAguaCritica = porLinha[idxCritica];
        System.out.println("Linha crítica: " + idxCritica + " (" + qtdAguaCritica + " cm)");

        int[] regioes = analisarRegioes(alerta);
        System.out.println("Regioes alagadas: " + regioes[0]);
        System.out.println("Maior regiao: " + regioes[1] + " celulas");

        // Roda a simulação errada (ingênua) só para comparar os resultados da parte D
        int a = rmEquipe % n;
        int b = (rmEquipe / 7) % m;
        int d1 = guardadaD1[a][b];

        int[][] errada = copiar(lamInicial);
        for (int p = 1; p <= passoD; p++) {
            errada = simularPassoIngenuo(alt, errada, dLin, dCol);
        }
        int d2 = errada[a][b];

        System.out.println("RESUMO RM=" + rmEquipe);
        System.out.println("ALERTA=" + numAlertas);
        System.out.println("CRITICA=" + idxCritica + " AGUA=" + qtdAguaCritica);
        System.out.println("D1=" + d1 + " D2=" + d2);

        sc.close();
    }

    // Cria uma cópia real da matriz. Se a gente só fizesse "matrizA = matrizB", mexer em uma alteraria a outra junto.
    public static int[][] copiar(int[][] matriz) {
        int lin = matriz.length;
        int col = matriz[0].length;
        int[][] aux = new int[lin][col];
        for (int i = 0; i < lin; i++) {
            for (int j = 0; j < col; j++) {
                aux[i][j] = matriz[i][j];
            }
        }
        return aux;
    }

    // Define para onde olhar nas colunas dependendo da ordem de desempate da equipe (N,L,S,O ou O,S,L,N)
    public static int[] criarDeslocamentoColuna(int ordem) {
        int[] v = new int[4];
        if (ordem == 0) {
            v[0] = 0;
            v[1] = 1;
            v[2] = 0;
            v[3] = -1;
        } else {
            v[0] = -1;
            v[1] = 0;
            v[2] = 1;
            v[3] = 0;
        }
        return v;
    }

    // Faz a soma de água por cada linha do bairro para descobrirmos qual faixa está sofrendo mais
    public static int[] calcularAguaPorLinha(int[][] lamina) {
        int[] somaLinhas = new int[lamina.length];
        for (int i = 0; i < lamina.length; i++) {
            int s = 0;
            for (int j = 0; j < lamina[0].length; j++) {
                s = s + lamina[i][j];
            }
            somaLinhas[i] = s;
        }
        return somaLinhas;
    }

    // Simula um ciclo completo de escoamento. Lê tudo da matriz atual e salva as mudanças numa cópia para tudo acontecer ao mesmo tempo.
    public static int[][] simularPasso(int[][] altitude, int[][] lamina,
                                       int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = altitude.length;
        int m = altitude[0].length;
        int[][] prox = copiar(lamina);
        int t, nivelEu, menorNivel, destI, destJ, vizI, vizJ, nivelViz;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                t = lamina[i][j] / 2;
                if (t > 0) {
                    nivelEu = altitude[i][j] + lamina[i][j];
                    menorNivel = nivelEu;
                    destI = -1;
                    destJ = -1;

                    // Procura qual vizinho válido tem o nível total mais baixo
                    for (int d = 0; d < 4; d++) {
                        vizI = i + deslocamentoLinha[d];
                        vizJ = j + deslocamentoColuna[d];

                        if (dentro(vizI, vizJ, n, m) == true) {
                            nivelViz = altitude[vizI][vizJ] + lamina[vizI][vizJ];
                            if (nivelViz < menorNivel) {
                                menorNivel = nivelViz;
                                destI = vizI;
                                destJ = vizJ;
                            }
                        }
                    }

                    // Se achou um lugar mais baixo, manda metade da água para lá
                    if (destI != -1) {
                        prox[i][j] = prox[i][j] - t;
                        prox[destI][destJ] = prox[destI][destJ] + t;
                    }
                }
            }
        }
        return prox;
    }

    // Gera a matriz inicial de chuva sorteando valores entre o mínimo e o máximo
    public static int[][] gerarMatriz(Random gerador, int n, int m, int minimo, int maximo) {
        int[][] chuva = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                chuva[i][j] = minimo + gerador.nextInt(maximo - minimo + 1);
            }
        }
        return chuva;
    }

    // Soma os RMs digitados para gerar a semente única da equipe
    public static int calcularRmEquipe(int[] rms) {
        int total = 0;
        for (int i = 0; i < rms.length; i++) {
            total = total + rms[i];
        }
        return total;
    }

    // Desenha o mapa visual da Defesa Civil usando '#' para pontos alagados e '.' para pontos normais
    public static void imprimirMapaAlerta(boolean[][] alerta) {
        System.out.println("Mapa de alerta (# = alagado, . = normal):");
        for (int i = 0; i < alerta.length; i++) {
            String linha = "";
            for (int j = 0; j < alerta[0].length; j++) {
                if (alerta[i][j] == true) {
                    linha = linha + "#";
                } else {
                    linha = linha + ".";
                }
                if (j < alerta[0].length - 1) {
                    linha = linha + "\t";
                }
            }
            System.out.println(linha);
        }
    }

    // Verifica qual linha acumulou mais água. Como usamos '>' estrito, em caso de empate vence a linha de menor índice.
    public static int encontrarLinhaCritica(int[] aguaPorLinha) {
        int maiorIdx = 0;
        for (int i = 1; i < aguaPorLinha.length; i++) {
            if (aguaPorLinha[i] > aguaPorLinha[maiorIdx]) {
                maiorIdx = i;
            }
        }
        return maiorIdx;
    }

    // Aplica as fórmulas de resto e divisão inteira sobre o RM da equipe para definir tamanho do mapa, ciclos, alerta e ordem
    public static int[] calcularParametros(int rm) {
        int[] resp = new int[5];
        resp[0] = 8 + (rm % 3);
        resp[1] = 10 + ((rm / 10) % 3);
        resp[2] = 4 + ((rm / 100) % 3);
        resp[3] = 10 + 2 * (rm % 4);
        resp[4] = rm % 2;
        return resp;
    }

    // Cria o relevo do bairro em formato de vale (mais alto nas bordas, mais baixo no meio) com uma variação aleatória
    public static int[][] gerarAltitude(Random gerador, int n, int m) {
        int[][] chao = new int[n][m];
        int meio = n / 2;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int dist = Math.abs(i - meio);
                chao[i][j] = 3 * dist + gerador.nextInt(5);
            }
        }
        return chao;
    }

    // Conta quantas ilhas (regiões conectadas) de alagamento existem e o tamanho da maior delas.
    // Como não podemos usar recursão nem listas prontas aqui, fizemos uma busca usando vetores para simular uma pilha.
    public static int[] analisarRegioes(boolean[][] alerta) {
        int n = alerta.length;
        int m = alerta[0].length;
        boolean[][] visitada = new boolean[n][m];
        int[] pilhaI = new int[n * m];
        int[] pilhaJ = new int[n * m];
        int[] dLin = {-1, 0, 1, 0};
        int[] dCol = {0, 1, 0, -1};
        int quantidade = 0;
        int maior = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (alerta[i][j] == true && visitada[i][j] == false) {
                    quantidade++;
                    int tamanho = 0;
                    int topo = 0;
                    pilhaI[topo] = i;
                    pilhaJ[topo] = j;
                    topo++;
                    visitada[i][j] = true;

                    // Vai empilhando e visitando os vizinhos alagados até acabar essa região
                    while (topo > 0) {
                        topo--;
                        int ci = pilhaI[topo];
                        int cj = pilhaJ[topo];
                        tamanho++;
                        for (int d = 0; d < 4; d++) {
                            int vi = ci + dLin[d];
                            int vj = cj + dCol[d];
                            if (dentro(vi, vj, n, m) == true && alerta[vi][vj] == true
                                    && visitada[vi][vj] == false) {
                                visitada[vi][vj] = true;
                                pilhaI[topo] = vi;
                                pilhaJ[topo] = vj;
                                topo++;
                            }
                        }
                    }

                    if (tamanho > maior) {
                        maior = tamanho;
                    }
                }
            }
        }
        int[] resp = new int[2];
        resp[0] = quantidade;
        resp[1] = maior;
        return resp;
    }

    // Garante que a coordenada (i, j) não saiu dos limites da matriz para evitar erro de índice
    public static boolean dentro(int i, int j, int n, int m) {
        if (i < 0 || i >= n) {
            return false;
        }
        if (j < 0 || j >= m) {
            return false;
        }
        return true;
    }

    // Imprime qualquer matriz formatada com tabulação para ficar alinhada no console
    public static void imprimir(int[][] matriz, String titulo) {
        System.out.println(titulo);
        for (int i = 0; i < matriz.length; i++) {
            String s = "";
            for (int j = 0; j < matriz[0].length; j++) {
                s = s + matriz[i][j];
                if (j < matriz[0].length - 1) {
                    s = s + "\t";
                }
            }
            System.out.println(s);
        }
    }

    // Versão "bugada" propositalmente para o item D2: altera a própria matriz enquanto percorre,
    // fazendo com que a água escoe várias vezes no mesmo ciclo em efeito cascata.
    public static int[][] simularPassoIngenuo(int[][] altitude, int[][] lamina,
                                              int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = altitude.length;
        int m = altitude[0].length;
        int t, nivelEu, menorNivel, destI, destJ, vizI, vizJ, nivelViz;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                t = lamina[i][j] / 2;
                if (t > 0) {
                    nivelEu = altitude[i][j] + lamina[i][j];
                    menorNivel = nivelEu;
                    destI = -1;
                    destJ = -1;

                    for (int d = 0; d < 4; d++) {
                        vizI = i + deslocamentoLinha[d];
                        vizJ = j + deslocamentoColuna[d];

                        if (dentro(vizI, vizJ, n, m) == true) {
                            nivelViz = altitude[vizI][vizJ] + lamina[vizI][vizJ];
                            if (nivelViz < menorNivel) {
                                menorNivel = nivelViz;
                                destI = vizI;
                                destJ = vizJ;
                            }
                        }
                    }

                    if (destI != -1) {
                        lamina[i][j] = lamina[i][j] - t;
                        lamina[destI][destJ] = lamina[destI][destJ] + t;
                    }
                }
            }
        }
        return lamina;
    }

    // Cria uma matriz de verdadeiro/falso marcando onde a água atingiu ou passou do limite de alerta
    public static boolean[][] marcarAlerta(int[][] lamina, int limiarAlerta) {
        int n = lamina.length;
        int m = lamina[0].length;
        boolean[][] mapa = new boolean[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (lamina[i][j] >= limiarAlerta) {
                    mapa[i][j] = true;
                } else {
                    mapa[i][j] = false;
                }
            }
        }
        return mapa;
    }

    // Define para onde olhar nas linhas dependendo da ordem de desempate da equipe
    public static int[] criarDeslocamentoLinha(int ordem) {
        int[] v = new int[4];
        if (ordem == 0) {
            v[0] = -1;
            v[1] = 0;
            v[2] = 1;
            v[3] = 0;
        } else {
            v[0] = 0;
            v[1] = 1;
            v[2] = 0;
            v[3] = -1;
        }
        return v;
    }
}