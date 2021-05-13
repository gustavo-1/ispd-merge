package ispd.arquivo.interpretador.gerador;

import javax.swing.JOptionPane;

class Interpretador{

    public boolean verbose;
    private String textoVerbose = "Saida do Verbose:";
    public boolean erroEncontrado = false;
    private String erros = "Erros encontrados durante o parser do Gerador:";

    //booleanos para controle
    private boolean dinamico = false;
    private boolean tarefaCrescente = true;
    private boolean recursoCrescente = true;

    //dados para costruir classe
    private String arquivoNome;
    private String pacote = "package ispd.externo;\n\n";
    private String imports = "import ispd.escalonador.Escalonador;\n"
            +"import ispd.motor.filas.Tarefa;\n"
            +"import ispd.motor.filas.servidores.CS_Processamento;\n"
            +"import ispd.motor.filas.servidores.CentroServico;\n"
            +"import java.util.List;\n"
            +"import java.util.ArrayList;\n\n";
    private String declaracao;
    private String variavel = "private Tarefa tarefaSelecionada = null;\n";
    private String construtor = "";
    private String caracteristica = "";
    private String decIniciar = "@Override\npublic void iniciar() {\n";
    private String iniciar = "";
    private String decTarefa = "@Override\n"
            +"public Tarefa escalonarTarefa() {\n";
    private String tarefa = "";
    private String tarefaExpressao = "";
    private String declararVariaveisTarefa = "";
    private String carregarVariaveisTarefa = "";
    private String decRecurso = "@Override\n"
            +"public CS_Processamento escalonarRecurso() {\n";
    private String recurso = "";
    private String recursoExpressao = "";
    private String declararVariaveisRecurso = "";
    private String carregarVariaveisRecurso = "";
    private String decEscalonar = "@Override\n"
            +"public void escalonar() {\n";
    private String escalonar =    "    tarefaSelecionada = escalonarTarefa();\n"
            +"    if(tarefaSelecionada != null){\n";
    private String ifEscalonar =  "        CentroServico rec = escalonarRecurso();\n"
            +"        tarefaSelecionada.setLocalProcessamento(rec);\n"
            +"        tarefaSelecionada.setCaminho(escalonarRota(rec));\n"
            +"        mestre.enviarTarefa(tarefaSelecionada);\n";
    //private String decResultadoAtualizar = "";
    //private String resultadoAtualizar = "";
    //private String fimResultadoAtualizar = "";
    private String decAddTarefaConcluida = "";
    private String addTarefaConcluida = "";
    private String fimAddTarefaConcluida = "";
    private String adicionarTarefa = "";
    private String getTempoAtualizar = "";
    private String rota = "@Override\n"
            +"public List<CentroServico> escalonarRota(CentroServico destino) {\n"
            +"    int index = escravos.indexOf(destino);\n"
            +"    return new ArrayList<CentroServico>((List<CentroServico>) caminhoEscravo.get(index));\n"
            +"}\n\n";
    private String metodosPrivate = "";

    public void resetaObjetosParser(){
        textoVerbose = "";
        erroEncontrado = false;
    }

    public void printv(String msg){
        textoVerbose = textoVerbose+"\n>"+msg;
    }

    public void addErro(String msg){
        erros = erros+"\n"+msg;
    }

    public void resuladoParser(){
        if (erroEncontrado) {
            JOptionPane.showMessageDialog(null, erros, "Found Errors", JOptionPane.ERROR_MESSAGE);
        } else if(verbose){
            JOptionPane.showMessageDialog(null, textoVerbose, "Saida do Reconhecimento", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void consomeTokens(){
        Token t = getToken(1);
        while( t.kind != SCHEDULER && t.kind != STATIC && t.kind != DYNAMIC && t.kind != TASK && t.kind != RESOURCE && t.kind != EOF){
            getNextToken();
            t = getToken(1);
        }
    }

    public void escreverNome(String text){
        arquivoNome = text;
        declaracao = "public class "+text+" extends Escalonador{\n\n";
        construtor = "public "+text+"() {\n"
                +"    this.tarefas = new ArrayList<Tarefa>();\n"
                +"    this.escravos = new ArrayList<CS_Processamento>();\n"
                +"}\n\n";
    }

    public void estatico(){
        caracteristica = "";
        dinamico = false;
    }

    public void dinamico(String tipo){
        if(tipo.equals("in")){
            adicionarTarefa = "@Override\n"
                    +"public void adicionarTarefa(Tarefa tarefa){\n"
                    +"    super.adicionarTarefa(tarefa);\n"
                    +"    for(CS_Processamento maq : this.getEscravos()){\n"
                    +"        mestre.atualizar(maq);\n"
                    +"    }\n"
                    +"}\n\n";
        }
        if(tipo.equals("out")){
            ifEscalonar = "    for(CS_Processamento maq : this.getEscravos()){\n"
                    +"        mestre.atualizar(maq);\n"
                    +"    }\n";
        }
        if(tipo.equals("end")){
            decAddTarefaConcluida =   "@Override\n"
                    +"public void addTarefaConcluida(Tarefa tarefa) {\n"
                    +"    super.addTarefaConcluida(tarefa);\n";
            addTarefaConcluida +=     "    for(CS_Processamento maq : this.getEscravos()){\n"
                    +"        mestre.atualizar(maq);\n"
                    +"    }\n";
            fimAddTarefaConcluida =   "}\n\n";

        }
    }

    public void dinamicoIntervalo(String text){
        getTempoAtualizar = "@Override\n"
                +"public Double getTempoAtualizar(){\n"
                +"    return (double) "+text+";\n"
                +"}\n\n";
    }

    public void formulaTarefa(String valor){
        if("random".equals(valor)){
            if(!imports.contains("import java.util.Random;")){
                imports = "import java.util.Random;\n" + imports;
            }
            if(!variavel.contains("private Random sorteio = new Random();")){
                variavel += "private Random sorteio = new Random();\n";
            }
            tarefa = "  if (!tarefas.isEmpty()) {\n"
                    +"      int tar = sorteio.nextInt(tarefas.size());\n"
                    +"      return tarefas.remove(tar);\n"
                    +"  }\n"
                    +"  return null;\n";
        }else if("fifo".equals(valor)){
            tarefa = "  if (!tarefas.isEmpty()) {\n"
                    +"      return tarefas.remove(0);\n"
                    +"  }\n"
                    +"  return null;\n";
        }else if("formula".equals(valor)){
            String ordenac = " < ";
            if(tarefaCrescente) ordenac = " > ";
            tarefa = "if(!tarefas.isEmpty()){\n"
                    + declararVariaveisTarefa
                    + "  double resultado = "+tarefaExpressao+";\n"
                    + "  int tar = 0;\n"
                    + "  for(int i = 0; i < tarefas.size(); i++){\n"
                    + carregarVariaveisTarefa
                    + "    double expressao = "+tarefaExpressao+";\n"
                    + "    if(resultado "+ordenac+" expressao){\n"
                    + "       resultado = expressao;\n"
                    + "       tar = i;\n"
                    + "    }\n"
                    + "  }\n"
                    + "return tarefas.remove(tar);\n"
                    + "}\n"
                    + "return null;\n";
        }
    }

    public void formulaRecurso(String valor){
        if("random".equals(valor)){
            if(!imports.contains("import java.util.Random;")){
                imports = "import java.util.Random;\n" + imports;
            }
            if(!variavel.contains("private Random sorteio = new Random();")){
                variavel += "Random sorteio = new Random();\n";
            }
            recurso = "  int rec = sorteio.nextInt(escravos.size());\n"
                    +"  return escravos.get(rec);\n";
        }else if("fifo".equals(valor)){
            if(!imports.contains("import java.util.ListIterator;")){
                imports = "import java.util.ListIterator;\n" + imports;
            }
            if(!variavel.contains("private ListIterator<CS_Processamento> recursos;")){
                variavel += "private ListIterator<CS_Processamento> recursos;\n";
            }
            if(!iniciar.contains("recursos = escravos.listIterator(0);")){
                iniciar += "    recursos = escravos.listIterator(0);\n";
            }
            recurso = "  if(!escravos.isEmpty()){\n"
                    +"      if (recursos.hasNext()) {\n"
                    +"          return recursos.next();\n"
                    +"      }else{\n"
                    +"          recursos = escravos.listIterator(0);\n"
                    +"          return recursos.next();\n"
                    +"      }\n"
                    +"  }\n"
                    +"  return null;\n";
        }else if("formula".equals(valor)){
            String ordenac = " < ";
            if(recursoCrescente) ordenac = " > ";
            recurso = "if(!escravos.isEmpty()){\n"
                    + declararVariaveisRecurso
                    + "  double resultado = "+recursoExpressao+";\n"
                    + "  int rec = 0;\n"
                    + "  for(int i = 0; i < escravos.size(); i++){\n"
                    + carregarVariaveisRecurso
                    + "    double expressao = "+recursoExpressao+";\n"
                    + "    if(resultado "+ordenac+" expressao){\n"
                    + "       resultado = expressao;\n"
                    + "       rec = i;\n"
                    + "    }\n"
                    + "  }\n"
                    + "return escravos.get(rec);\n"
                    + "}\n"
                    + "return null;\n";
        }
    }

    public void addConstanteTarefa(String valor) {
        tarefaExpressao += valor;
    }

    public void addConstanteRecurso(String valor) {
        recursoExpressao += valor;
    }

    public void limite(String valorInteiro, boolean porRecurso){
        if(porRecurso){
            metodosPrivate += "private boolean condicoesEscalonamento() {\n"
                    +"    int cont = 1;\n"
                    +"    for (List tarefasNoRecurso : tarExecRec) {\n"
                    +"        if (tarefasNoRecurso.size() > 1) {\n"
                    +"            cont++;\n"
                    +"        }\n"
                    +"    }\n"
                    +"    if(cont >= tarExecRec.size()){\n"
                    +"        mestre.setTipoEscalonamento(mestre.QUANDO_RECEBE_RESULTADO);\n"
                    +"        return false;\n"
                    +"    }\n"
                    +"    mestre.setTipoEscalonamento(mestre.ENQUANTO_HOUVER_TAREFAS);\n"
                    +"    return true;\n"
                    +"}\n\n";
            if(!variavel.contains("tarExecRec")){
                variavel += "private List<List> tarExecRec;\n";
            }
            if(!iniciar.contains("tarExecRec")){
                iniciar += "    tarExecRec = new ArrayList<List>(escravos.size());\n"
                        +"    for (int i = 0; i < escravos.size(); i++) {\n"
                        +"        tarExecRec.add(new ArrayList<Tarefa>());\n"
                        +"    }\n";
            }
            if(!metodosPrivate.contains("private void addTarefasEnviadas(){")){
                metodosPrivate += "private void addTarefasEnviadas(){\n"
                        +"    if(tarefaSelecionada != null){\n"
                        +"        int index = escravos.indexOf(tarefaSelecionada.getLocalProcessamento());\n"
                        +"        tarExecRec.get(index).add(tarefaSelecionada);\n"
                        +"    }\n"
                        +"}\n\n";
            }
            if(!escalonar.contains("if (condicoesEscalonamento())")){
                escalonar = "tarefaSelecionada = null;\n"
                        +"if (condicoesEscalonamento())\n"
                        + escalonar;
            }
            if(!ifEscalonar.contains("addTarefasEnviadas();")){
                ifEscalonar += "addTarefasEnviadas();\n";
            }
            decAddTarefaConcluida =   "@Override\n"
                    +"public void addTarefaConcluida(Tarefa tarefa) {\n"
                    +"    super.addTarefaConcluida(tarefa);\n";
            addTarefaConcluida =      addTarefaConcluida
                    +"    for (int i = 0; i < escravos.size(); i++) {\n"
                    +"        if (tarExecRec.get(i).contains(tarefa)) {\n"
                    +"            tarExecRec.get(i).remove(tarefa);\n"
                    +"        }\n"
                    +"    }\n";
            fimAddTarefaConcluida =   "}\n\n";
        } else {
            metodosPrivate += "private boolean  condicoesEscalonamento() {\n"
                    +"    int cont = 1;\n"
                    +"    for (String usuario : metricaUsuarios.getUsuarios()) {\n"
                    +"        if( (metricaUsuarios.getSizeTarefasSubmetidas(usuario) - metricaUsuarios.getSizeTarefasConcluidas(usuario) ) > "+valorInteiro+"){\n"
                    +"            cont++;\n"
                    +"        }\n"
                    +"    }\n"
                    +"    if(cont >= metricaUsuarios.getUsuarios().size()){\n"
                    +"        mestre.setTipoEscalonamento(mestre.QUANDO_RECEBE_RESULTADO);\n"
                    +"        return false;\n"
                    +"    }\n"
                    +"    mestre.setTipoEscalonamento(mestre.ENQUANTO_HOUVER_TAREFAS);\n"
                    +"    return true;\n"
                    +"}\n\n";
        }
    }

    public void addExpressaoTarefa(int tipoToken){
        switch (tipoToken) {
            case add:
                tarefaExpressao += " + ";
                break;
            case sub:
                tarefaExpressao += " - ";
                break;
            case div:
                tarefaExpressao += " / ";
                break;
            case mult:
                tarefaExpressao += " * ";
                break;
            case lparen:
                tarefaExpressao += " ( ";
                break;
            case rparen:
                tarefaExpressao += " ) ";
                break;
            case tTamComp:
                tarefaExpressao += "tTamComp";
                if(!declararVariaveisTarefa.contains("tTamComp")){
                    declararVariaveisTarefa += "double tTamComp = tarefas.get(0).getTamProcessamento();\n";
                }
                if(!carregarVariaveisTarefa.contains("tTamComp")){
                    carregarVariaveisTarefa += "tTamComp = tarefas.get(i).getTamProcessamento();\n";
                }
                break;
            case tTamComu:
                tarefaExpressao += "tTamComu";
                if(!declararVariaveisTarefa.contains("tTamComu")){
                    declararVariaveisTarefa += "double tTamComu = tarefas.get(0).getTamComunicacao();\n";
                }
                if(!carregarVariaveisTarefa.contains("tTamComu")){
                    carregarVariaveisTarefa += "tTamComu = tarefas.get(i).getTamComunicacao();\n";
                }
                break;
            case tTempSubm:
                tarefaExpressao += "tTempSubm";
                if(!declararVariaveisTarefa.contains("tTempSubm")){
                    declararVariaveisTarefa += "double tTempSubm = tarefas.get(0).getTimeCriacao();\n";
                }
                if(!carregarVariaveisTarefa.contains("tTempSubm")){
                    carregarVariaveisTarefa += "tTempSubm = tarefas.get(i).getTimeCriacao();\n";
                }
                break;
            case tNumTarSub:
                tarefaExpressao += "tNumTarSub";
                if(!declararVariaveisTarefa.contains("tNumTarSub")){
                    if(dinamico)
                        declararVariaveisTarefa += "int tNumTarSub = mestre.getSimulacao().getRedeDeFilas().getMetricasUsuarios().getSizeTarefasSubmetidas(tarefas.get(0).getProprietario());\n";
                    else
                        declararVariaveisTarefa += "int tNumTarSub = metricaUsuarios.getSizeTarefasSubmetidas(tarefas.get(0).getProprietario());\n";
                }
                if(!carregarVariaveisTarefa.contains("tNumTarSub")){
                    if(dinamico)
                        carregarVariaveisTarefa += "tNumTarSub = mestre.getSimulacao().getRedeDeFilas().getMetricasUsuarios().getSizeTarefasSubmetidas(tarefas.get(i).getProprietario());\n";
                    else
                        carregarVariaveisTarefa += "tNumTarSub = metricaUsuarios.getSizeTarefasSubmetidas(tarefas.get(i).getProprietario());\n";
                }
                break;
            case tNumTarConc:
                tarefaExpressao += "tNumTarConc";
                if(!declararVariaveisTarefa.contains("tNumTarConc")){
                    if(dinamico)
                        declararVariaveisTarefa += "int tNumTarConc = mestre.getSimulacao().getRedeDeFilas().getMetricasUsuarios().getSizeTarefasConcluidas(tarefas.get(0).getProprietario());\n";
                    else
                        declararVariaveisTarefa += "int tNumTarConc = metricaUsuarios.getSizeTarefasConcluidas(tarefas.get(0).getProprietario());\n";
                }
                if(!carregarVariaveisTarefa.contains("tNumTarConc")){
                    if(dinamico)
                        carregarVariaveisTarefa += "tNumTarConc = mestre.getSimulacao().getRedeDeFilas().getMetricasUsuarios().getSizeTarefasConcluidas(tarefas.get(i).getProprietario());\n";
                    else
                        carregarVariaveisTarefa += "tNumTarConc = metricaUsuarios.getSizeTarefasConcluidas(tarefas.get(i).getProprietario());\n";
                }
                break;
            case tPoderUser:
                tarefaExpressao += "tPoderUser";
                if(!declararVariaveisTarefa.contains("tPoderUser")){
                    declararVariaveisTarefa += "double tPoderUser = metricaUsuarios.getPoderComputacional(tarefas.get(0).getProprietario());\n";
                }
                if(!carregarVariaveisTarefa.contains("tPoderUser")){
                    carregarVariaveisTarefa += "tPoderUser = metricaUsuarios.getPoderComputacional(tarefas.get(i).getProprietario());\n";
                }
                break;
            default:
                Token t = getToken(1);
                addErro("Erro semantico encontrado na linha "+t.endLine+", coluna "+t.endColumn);
                erroEncontrado = true;
                consomeTokens();
                resuladoParser();
        }
    }

    public void addExpressaoRecurso(int tipoToken){
        switch (tipoToken) {
            case add:
                recursoExpressao += " + ";
                break;
            case sub:
                recursoExpressao += " - ";
                break;
            case div:
                recursoExpressao += " / ";
                break;
            case mult:
                recursoExpressao += " * ";
                break;
            case lparen:
                recursoExpressao += " ( ";
                break;
            case rparen:
                recursoExpressao += " ) ";
                break;
            case rPodeProc:
                recursoExpressao += "rPodeProc";
                if(!declararVariaveisRecurso.contains("rPodeProc")){
                    declararVariaveisRecurso += "double rPodeProc = escravos.get(0).getPoderComputacional();\n";
                }
                if(!carregarVariaveisRecurso.contains("rPodeProc")){
                    carregarVariaveisRecurso += "rPodeProc = escravos.get(i).getPoderComputacional();\n";
                }
                break;
            case rLinkComu:
                recursoExpressao += "rLinkComu";
                if(!imports.contains("import ispd.motor.filas.servidores.CS_Comunicacao;")){
                    imports = "import ispd.motor.filas.servidores.CS_Comunicacao;\n" + imports;
                }
                if(!declararVariaveisRecurso.contains("rLinkComu")){
                    declararVariaveisRecurso += "double rLinkComu = calcularBandaLink(escravos.get(0));\n";
                }
                if(!carregarVariaveisRecurso.contains("rLinkComu")){
                    carregarVariaveisRecurso += "rLinkComu = calcularBandaLink(escravos.get(i));\n";
                }
                if(!metodosPrivate.contains("private double calcularBandaLink(CS_Processamento get)")){
                    metodosPrivate += "private double calcularBandaLink(CS_Processamento get) {\n"
                            +"double total = 0;\n"
                            +"int conec = 0;\n"
                            +"for (CentroServico cs : escalonarRota(get)) {\n"
                            +"    if(cs instanceof CS_Comunicacao){\n"
                            +"         CS_Comunicacao comu = (CS_Comunicacao) cs;\n"
                            +"         total += comu.getLarguraBanda();\n"
                            +"         conec++;\n"
                            +"    }\n"
                            +"}\n"
                            +"return total / conec;\n"
                            +"}\n\n";
                }
                break;
            case rtamCompTar:
                recursoExpressao += "rtamCompTar";
                if(!declararVariaveisRecurso.contains("rtamCompTar")){
                    declararVariaveisRecurso += "double rtamCompTar = tarefaSelecionada.getTamProcessamento();\n";
                }
                break;
            case rtamComuTar:
                recursoExpressao += "rtamComuTar";
                if(!declararVariaveisRecurso.contains("rtamComuTar")){
                    declararVariaveisRecurso += "double rtamComuTar = tarefaSelecionada.getTamComunicacao();\n";
                }
                break;
            case numTarExec:
                recursoExpressao += "numTarExec";
                if(!variavel.contains("numTarExecRec")){
                    variavel += "private List<Integer> numTarExecRec;\n";
                }
                if(!variavel.contains("tarExecRec")){
                    variavel += "private List<List> tarExecRec;\n";
                }
                if(!metodosPrivate.contains("private void addTarefasEnviadasNum(){")){
                    metodosPrivate += "private void addTarefasEnviadasNum(){\n"
                            +"    if(tarefaSelecionada != null){\n"
                            +"        int index = escravos.indexOf(tarefaSelecionada.getLocalProcessamento());\n"
                            +"        numTarExecRec.set(index,numTarExecRec.get(index)+1);\n"
                            +"    }\n"
                            +"}\n\n";
                }
                if(!metodosPrivate.contains("private void addTarefasEnviadas(){")){
                    metodosPrivate += "private void addTarefasEnviadas(){\n"
                            +"    if(tarefaSelecionada != null){\n"
                            +"        int index = escravos.indexOf(tarefaSelecionada.getLocalProcessamento());\n"
                            +"        tarExecRec.get(index).add(tarefaSelecionada);\n"
                            +"    }\n"
                            +"}\n\n";
                }
                if(!ifEscalonar.contains("addTarefasEnviadasNum();")){
                    ifEscalonar += "addTarefasEnviadasNum();\n";
                }
                if(!ifEscalonar.contains("addTarefasEnviadas();")){
                    ifEscalonar += "addTarefasEnviadas();\n";
                }
                if(!imports.contains("import java.util.ArrayList;")){
                    imports = "import java.util.ArrayList;\n" + imports;
                }
                if(!iniciar.contains("numTarExecRec")){
                    iniciar += "    numTarExecRec = new ArrayList<Integer>(escravos.size());\n"
                            +"    for (int i = 0; i < escravos.size(); i++) {\n"
                            +"        numTarExecRec.add(0);\n"
                            +"    }\n";
                }
                if(!iniciar.contains("tarExecRec")){
                    iniciar += "    tarExecRec = new ArrayList<List>(escravos.size());\n"
                            +"    for (int i = 0; i < escravos.size(); i++) {\n"
                            +"        tarExecRec.add(new ArrayList<Tarefa>());\n"
                            +"    }\n";
                }
                if(!addTarefaConcluida.contains("numTarExecRec")){
                    decAddTarefaConcluida = "@Override\n"
                            +"public void addTarefaConcluida(Tarefa tarefa) {\n"
                            +"    super.addTarefaConcluida(tarefa);\n";
                    addTarefaConcluida =   "    int index = escravos.indexOf(tarefa.getLocalProcessamento());\n"
                            +"    if(index != -1){\n"
                            +"        numTarExecRec.set(index, numTarExecRec.get(index) - 1);\n"
                            +"    } else {\n"
                            +"        for(int i = 0; i < escravos.size(); i++){\n"
                            +"            if (tarExecRec.get(i).contains(tarefa)) {\n"
                            +"                numTarExecRec.set(i, numTarExecRec.get(i) - 1);\n"
                            +"                tarExecRec.get(i).remove(tarefa);\n"
                            +"            }\n"
                            +"        }\n"
                            +"    }\n" + addTarefaConcluida;
                    fimAddTarefaConcluida = "}\n\n";
                }
                if(!declararVariaveisRecurso.contains("numTarExec")){
                    if(dinamico)
                        declararVariaveisRecurso += "int numTarExec = numTarExecRec.get(0) + escravos.get(0).getInformacaoDinamicaFila().size();\n";
                    else
                        declararVariaveisRecurso += "int numTarExec = numTarExecRec.get(0);\n";
                }
                if(!carregarVariaveisRecurso.contains("numTarExec")){
                    if(dinamico)
                        carregarVariaveisRecurso += "numTarExec = numTarExecRec.get(i) + escravos.get(i).getInformacaoDinamicaFila().size();\n";
                    else
                        carregarVariaveisRecurso += "numTarExec = numTarExecRec.get(i);\n";
                }
                break;
            case mflopProce:
                recursoExpressao += "mflopProce";
                if(!variavel.contains("mflopProceRec")){
                    variavel += "private List<Double> mflopProceRec;\n";
                }
                if(!variavel.contains("tarExecRec")){
                    variavel += "private List<List> tarExecRec;\n";
                }
                if(!ifEscalonar.contains("addTarefasEnviadasMflop();")){
                    ifEscalonar += "addTarefasEnviadasMflop();\n";
                }
                if(!ifEscalonar.contains("addTarefasEnviadas();")){
                    ifEscalonar += "addTarefasEnviadas();\n";
                }
                if(!iniciar.contains("mflopProceRec")){
                    iniciar += "    mflopProceRec = new ArrayList<Double>(escravos.size());\n"
                            +"    for (int i = 0; i < escravos.size(); i++) {\n"
                            +"        mflopProceRec.add(0.0);\n"
                            +"    }\n";
                }
                if(!iniciar.contains("tarExecRec")){
                    iniciar += "    tarExecRec = new ArrayList<List>(escravos.size());\n"
                            +"    for (int i = 0; i < escravos.size(); i++) {\n"
                            +"        tarExecRec.add(new ArrayList<Tarefa>());\n"
                            +"    }\n";
                }
                if(!metodosPrivate.contains("private void addTarefasEnviadasMflop(){")){
                    metodosPrivate += "private void addTarefasEnviadasMflop(){\n"
                            +"    if(tarefaSelecionada != null){\n"
                            +"        int index = escravos.indexOf(tarefaSelecionada.getLocalProcessamento());\n"
                            +"        mflopProceRec.set(index,mflopProceRec.get(index)+tarefaSelecionada.getTamProcessamento());\n"
                            +"    }\n"
                            +"}\n\n";
                }
                if(!metodosPrivate.contains("private void addTarefasEnviadas(){")){
                    metodosPrivate += "private void addTarefasEnviadas(){\n"
                            +"    if(tarefaSelecionada != null){\n"
                            +"        int index = escravos.indexOf(tarefaSelecionada.getLocalProcessamento());\n"
                            +"        tarExecRec.get(index).add(tarefaSelecionada);\n"
                            +"    }\n"
                            +"}\n\n";
                }
                if(!addTarefaConcluida.contains("mflopProceRec")){
                    decAddTarefaConcluida = "@Override\n"
                            +"public void addTarefaConcluida(Tarefa tarefa) {\n"
                            +"    super.addTarefaConcluida(tarefa);\n";
                    addTarefaConcluida  =   "    int index2 = escravos.indexOf(tarefa.getLocalProcessamento());\n"
                            +"    if(index2 != -1){\n"
                            +"        mflopProceRec.set(index2, mflopProceRec.get(index2) - tarefa.getTamProcessamento());\n"
                            +"    } else {\n"
                            +"        for(int i = 0; i < escravos.size(); i++){\n"
                            +"            if (tarExecRec.get(i).contains(tarefa)) {\n"
                            +"                mflopProceRec.set(i, mflopProceRec.get(i) - tarefa.getTamProcessamento());\n"
                            +"            }\n"
                            +"        }\n"
                            +"    }\n" + addTarefaConcluida;
                    fimAddTarefaConcluida = "}\n\n";
                }
                if(dinamico && !metodosPrivate.contains("private Double mflopsNoRecIndex(int index)")){
                    metodosPrivate += "private double mflopsNoRecIndex(int index) {\n"
                            +"    double mflops = 0;\n"
                            +"    for(Object tar : escravos.get(index).getInformacaoDinamicaFila()){\n"
                            +"        Tarefa tarefa = (Tarefa) tar;\n"
                            +"        mflops += tarefa.getTamProcessamento();\n"
                            +"    }\n"
                            +"    return mflops;\n"
                            +"}\n\n";
                }
                if(!declararVariaveisRecurso.contains("mflopProce")){
                    if(dinamico)
                        declararVariaveisRecurso += "double mflopProce = mflopProceRec.get(0) + mflopsNoRecIndex(0);\n";
                    else
                        declararVariaveisRecurso += "double mflopProce = mflopProceRec.get(0);\n";
                }
                if(!carregarVariaveisRecurso.contains("mflopProce")){
                    if(dinamico)
                        carregarVariaveisRecurso += "mflopProce = mflopProceRec.get(i) + mflopsNoRecIndex(i);\n";
                    else
                        carregarVariaveisRecurso += "mflopProce = mflopProceRec.get(i);\n";
                }
                break;
            default:
                Token t = getToken(1);
                addErro("Erro semantico encontrado na linha "+t.endLine+", coluna "+t.endColumn);
                erroEncontrado = true;
                consomeTokens();
                resuladoParser();
        }
    }

    public String getCodigo(){
        if( !"".equals(recursoExpressao) ){
            formulaRecurso("formula");
        }
        if( !"".equals(tarefaExpressao) ){
            formulaTarefa("formula");
        }
        String txt = pacote
                + imports
                + declaracao
                + variavel + "\n"
                + construtor
                + caracteristica
                + decIniciar
                + iniciar + "}\n\n"
                + decTarefa
                + tarefa + "}\n\n"
                + decRecurso
                + recurso + "}\n\n"
                + decEscalonar
                + escalonar
                + ifEscalonar + "    }\n}\n\n"
                //+ decResultadoAtualizar
                //+ resultadoAtualizar
                //+ fimResultadoAtualizar
                + decAddTarefaConcluida
                + addTarefaConcluida
                + fimAddTarefaConcluida
                + adicionarTarefa
                + getTempoAtualizar
                + metodosPrivate
                + rota + "}";
        return txt;
    }

    public void setTarefaCrescente(boolean valor){
        tarefaCrescente = valor;
    }

    public void setRecursoCrescente(boolean valor){
        recursoCrescente = valor;
    }

    public String getArquivoNome() {
        return arquivoNome;
    }

}
