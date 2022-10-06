package ispd.arquivo.xml;

import ispd.arquivo.xml.utils.SwitchConnection;
import ispd.motor.filas.RedeDeFilasCloud;
import ispd.motor.filas.servidores.CS_Processamento;
import ispd.motor.filas.servidores.CentroServico;
import ispd.motor.filas.servidores.implementacao.CS_MaquinaCloud;
import ispd.motor.filas.servidores.implementacao.CS_Switch;
import ispd.motor.filas.servidores.implementacao.CS_VMM;
import ispd.motor.filas.servidores.implementacao.CS_VirtualMac;
import ispd.motor.metricas.MetricasUsuarios;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudQueueNetworkBuilder extends QueueNetworkBuilder {
    private final NodeList docMachines;
    private final NodeList docClusters;
    private final NodeList docOwners;
    private final NodeList docVms;
    private final HashMap<CentroServico, List<CS_MaquinaCloud>> clusterSlaves =
            new HashMap<>(0);
    private final List<CS_MaquinaCloud> machines = new ArrayList<>(0);
    private final List<CS_VirtualMac> virtualMachines = new ArrayList<>(0);
    private final List<CS_Processamento> virtualMachineMasters =
            new ArrayList<>(0);

    public CloudQueueNetworkBuilder(final Document model) {
        super(new WrappedDocument(model));

        final var doc = new WrappedDocument(model);

        this.docMachines = model.getElementsByTagName("machine");
        this.docClusters = model.getElementsByTagName("cluster");
        this.docOwners = model.getElementsByTagName("owner");
        this.docVms = model.getElementsByTagName("virtualMac");

        processOwners();
        //cria maquinas, mestres, internets e mestres dos clusters
        //Realiza leitura dos icones de máquina
        processMachines();
        //Realiza leitura dos icones de cluster
        processClusters();

        doc.internets().forEach(this::processInternetElement);


        doc.links().forEach(this::processLinkElement);


        //adiciona os escravos aos mestres
        processMasters();

        //Realiza leitura dos ícones de máquina virtual
        processVirtualMachines();
    }

    private void processOwners() {
        for (int i = 0; i < this.docOwners.getLength(); i++) {
            final Element owner = (Element) this.docOwners.item(i);
            this.powerLimits.put(owner.getAttribute("id"), 0.0);
        }
    }

    private void processMachines() {
        for (int i = 0; i < this.docMachines.getLength(); i++) {
            final Element maquina = (Element) this.docMachines.item(i);
            final Element id =
                    GridBuilder.getFirstTagElement(maquina, "icon_id");
            final int global = Integer.parseInt(id.getAttribute("global"));
            if (new WrappedElement(maquina).hasMasterAttribute()) {
                final Element master =
                        GridBuilder.getFirstTagElement(maquina,
                                "master");
                final Element carac = GridBuilder.getFirstTagElement(maquina,
                        "characteristic");
                final Element proc =
                        GridBuilder.getFirstTagElement(carac, "process");
                final Element memoria = GridBuilder.getFirstTagElement(carac,
                        "memory");
                final Element disco = GridBuilder.getFirstTagElement(carac,
                        "hard_disk");
                final Element custo =
                        GridBuilder.getFirstTagElement(carac, "cost");
                //instancia o CS_VMM
                final CS_Processamento mestre = new CS_VMM(
                        maquina.getAttribute("id"),
                        maquina.getAttribute("owner"),
                        Double.parseDouble(proc.getAttribute("power")),
                        Double.parseDouble(memoria.getAttribute("size")),
                        Double.parseDouble(disco.getAttribute("size")),
                        Double.parseDouble(maquina.getAttribute("load")),
                        master.getAttribute("scheduler")/*Escalonador*/,
                        master.getAttribute("vm_alloc"));
                this.virtualMachineMasters.add(mestre);
                this.serviceCenters.put(global, mestre);
                //Contabiliza para o usuario poder computacional do mestre
                this.powerLimits.put(mestre.getProprietario(),
                        this.powerLimits.get(mestre.getProprietario()) + mestre.getPoderComputacional());
            } else {
                //acessa as características do máquina
                final Element caracteristica =
                        GridBuilder.getFirstTagElement(maquina,
                                "characteristic");
                final Element custo =
                        GridBuilder.getFirstTagElement(caracteristica,
                                "cost");
                final Element processamento =
                        GridBuilder.getFirstTagElement(caracteristica,
                                "process");
                final Element memoria =
                        GridBuilder.getFirstTagElement(caracteristica,
                                "memory");
                final Element disco =
                        GridBuilder.getFirstTagElement(caracteristica,
                                "hard_disk");
                //instancia um CS_MaquinaCloud
                final CS_MaquinaCloud maq = new CS_MaquinaCloud(
                        maquina.getAttribute("id"),
                        maquina.getAttribute("owner"),
                        Double.parseDouble(processamento.getAttribute(
                                "power")),
                        Integer.parseInt(processamento.getAttribute(
                                "number")),
                        Double.parseDouble(maquina.getAttribute("load")),
                        Double.parseDouble(memoria.getAttribute("size")),
                        Double.parseDouble(disco.getAttribute("size")),
                        Double.parseDouble(custo.getAttribute("cost_proc")),
                        Double.parseDouble(custo.getAttribute("cost_mem")),
                        Double.parseDouble(custo.getAttribute("cost_disk"))
                );
                this.machines.add(maq);
                this.serviceCenters.put(global, maq);
                this.powerLimits.put(maq.getProprietario(),
                        this.powerLimits.get(maq.getProprietario()) + maq.getPoderComputacional());
            }
        }
    }

    private void processClusters() {
        for (int i = 0; i < this.docClusters.getLength(); i++) {
            final Element cluster = (Element) this.docClusters.item(i);
            final Element id =
                    GridBuilder.getFirstTagElement(cluster, "icon_id");
            final Element carac = GridBuilder.getFirstTagElement(cluster,
                    "characteristic");
            final Element proc =
                    GridBuilder.getFirstTagElement(carac, "process");
            final Element mem =
                    GridBuilder.getFirstTagElement(carac, "memory");
            final Element disc =
                    GridBuilder.getFirstTagElement(carac, "hard_disk");

            final int global = Integer.parseInt(id.getAttribute("global"));
            if (Boolean.parseBoolean(cluster.getAttribute("master"))) {
                final CS_VMM clust = new CS_VMM(
                        cluster.getAttribute("id"),
                        cluster.getAttribute("owner"),
                        Double.parseDouble(proc.getAttribute("power")),
                        Double.parseDouble(mem.getAttribute("size")),
                        Double.parseDouble(disc.getAttribute("size")),
                        0.0,
                        cluster.getAttribute("scheduler")/*Escalonador*/,
                        cluster.getAttribute("vm_alloc"));
                this.virtualMachineMasters.add(clust);
                this.serviceCenters.put(global, clust);
                //Contabiliza para o usuario poder computacional do mestre
                final int numeroEscravos =
                        Integer.parseInt(cluster.getAttribute(
                                "nodes"));
                final double total =
                        clust.getPoderComputacional() + (clust.getPoderComputacional() * numeroEscravos);
                this.powerLimits.put(clust.getProprietario(),
                        total + this.powerLimits.get(clust.getProprietario()));
                final CS_Switch Switch = new CS_Switch(
                        (cluster.getAttribute("id") + "switch"),
                        Double.parseDouble(cluster.getAttribute(
                                "bandwidth")),
                        0.0,
                        Double.parseDouble(cluster.getAttribute("latency")));
                this.links.add(Switch);
                clust.addConexoesEntrada(Switch);
                clust.addConexoesSaida(Switch);
                Switch.addConexoesEntrada(clust);
                Switch.addConexoesSaida(clust);
                for (int j = 0; j < numeroEscravos; j++) {
                    final Element caracteristica =
                            GridBuilder.getFirstTagElement(cluster,
                                    "characteristic");
                    final Element custo =
                            GridBuilder.getFirstTagElement(caracteristica,
                                    "cost");
                    final Element processamento =
                            GridBuilder.getFirstTagElement(caracteristica,
                                    "process");
                    final Element memoria =
                            GridBuilder.getFirstTagElement(caracteristica,
                                    "memory");
                    final Element disco =
                            GridBuilder.getFirstTagElement(caracteristica,
                                    "hard_disk");
                    final var maq =
                            new CS_MaquinaCloud(
                                    "%s.%d".formatted(cluster.getAttribute(
                                            "id"), j),
                                    cluster.getAttribute("owner"),
                                    Double.parseDouble(processamento.getAttribute("power")),
                                    Integer.parseInt(processamento.getAttribute("number")),
                                    Double.parseDouble(memoria.getAttribute(
                                            "size")),
                                    Double.parseDouble(disco.getAttribute(
                                            "size")),
                                    Double.parseDouble(custo.getAttribute(
                                            "cost_proc")),
                                    Double.parseDouble(custo.getAttribute(
                                            "cost_mem")),
                                    Double.parseDouble(custo.getAttribute(
                                            "cost_disk")),
                                    0.0,
                                    j + 1
                            );

                    SwitchConnection.toCloudMachine(maq, Switch);

                    maq.addMestre(clust);
                    clust.addEscravo(maq);
                    this.machines.add(maq);
                    //não adicionei referencia ao switch nem aos escrevos do
                    // cluster aos centros de serviços
                }
            } else {
                final CS_Switch Switch = new CS_Switch(
                        (cluster.getAttribute("id") + "switch"),
                        Double.parseDouble(cluster.getAttribute(
                                "bandwidth")),
                        0.0,
                        Double.parseDouble(cluster.getAttribute("latency")));
                this.links.add(Switch);
                this.serviceCenters.put(global, Switch);
                //Contabiliza para o usuario poder computacional do mestre
                final double total =
                        Double.parseDouble(cluster.getAttribute(
                                "power"))
                        * Integer.parseInt(cluster.getAttribute(
                                "nodes"
                        ));
                this.powerLimits.put(cluster.getAttribute("owner"),
                        total + this.powerLimits.get(cluster.getAttribute(
                                "owner")));
                final List<CS_MaquinaCloud> maqTemp =
                        new ArrayList<>();
                final int numeroEscravos =
                        Integer.parseInt(cluster.getAttribute(
                                "nodes"));
                for (int j = 0; j < numeroEscravos; j++) {
                    final Element caracteristica =
                            (Element) cluster.getElementsByTagName(
                                    "characteristic");
                    final Element custo =
                            (Element) caracteristica.getElementsByTagName(
                                    "cost");
                    final Element processamento =
                            (Element) caracteristica.getElementsByTagName(
                                    "process");
                    final Element memoria =
                            (Element) caracteristica.getElementsByTagName(
                                    "memory");
                    final Element disco =
                            (Element) caracteristica.getElementsByTagName(
                                    "hard_disk");
                    final var maq =
                            new CS_MaquinaCloud(
                                    "%s.%d".formatted(cluster.getAttribute(
                                            "id"), j),
                                    cluster.getAttribute("owner"),
                                    Double.parseDouble(processamento.getAttribute("power")),
                                    Integer.parseInt(processamento.getAttribute("number")),
                                    Double.parseDouble(memoria.getAttribute(
                                            "size")),
                                    Double.parseDouble(disco.getAttribute(
                                            "size")),
                                    Double.parseDouble(custo.getAttribute(
                                            "cost_proc")),
                                    Double.parseDouble(custo.getAttribute(
                                            "cost_mem")),
                                    Double.parseDouble(custo.getAttribute(
                                            "cost_disk")),
                                    0.0,
                                    j + 1
                            );
                    SwitchConnection.toCloudMachine(maq, Switch);
                    maqTemp.add(maq);
                    this.machines.add(maq);
                }
                this.clusterSlaves.put(Switch, maqTemp);
            }
        }
    }

    private void processMasters() {
        for (int i = 0; i < this.docMachines.getLength(); i++) {
            final Element maquina = (Element) this.docMachines.item(i);
            final Element id =
                    GridBuilder.getFirstTagElement(maquina, "icon_id");
            final int global = Integer.parseInt(id.getAttribute("global"));
            if (new WrappedElement(maquina).hasMasterAttribute()) {
                final Element master =
                        GridBuilder.getFirstTagElement(maquina,
                                "master");
                final NodeList slaves = master.getElementsByTagName(
                        "slave");
                final CS_VMM mestre = (CS_VMM) this.serviceCenters.get(global);
                for (int j = 0; j < slaves.getLength(); j++) {
                    final Element slave = (Element) slaves.item(j);
                    final CentroServico maq =
                            this.serviceCenters.get(Integer.parseInt(slave.getAttribute("id")));
                    if (maq instanceof CS_Processamento) {
                        mestre.addEscravo((CS_Processamento) maq);
                        if (maq instanceof CS_MaquinaCloud maqTemp) {
                            //trecho de debbuging
                            System.out.println(maqTemp.getId() + " " +
                                               "adicionou " +
                                               "como mestre: " + mestre.getId());
                            //fim dbg
                            maqTemp.addMestre(mestre);
                        }
                    } else if (maq instanceof CS_Switch) {
                        for (final CS_MaquinaCloud escr :
                                this.clusterSlaves.get(maq)) {
                            escr.addMestre(mestre);
                            mestre.addEscravo(escr);
                        }
                    }
                }
            }
        }
    }

    private void processVirtualMachines() {
        for (int i = 0; i < this.docVms.getLength(); i++) {
            final Element virtualMac = (Element) this.docVms.item(i);
            final CS_VirtualMac VM =
                    new CS_VirtualMac(virtualMac.getAttribute("id"),
                            virtualMac.getAttribute("owner"),
                            Integer.parseInt(virtualMac.getAttribute(
                                    "power")),
                            Double.parseDouble(virtualMac.getAttribute(
                                    "mem_alloc")),
                            Double.parseDouble(virtualMac.getAttribute(
                                    "disk_alloc")),
                            virtualMac.getAttribute("op_system"));
            //adicionando VMM responsável pela VM
            for (final CS_Processamento aux : this.virtualMachineMasters) {
                if (virtualMac.getAttribute("vmm").equals(aux.getId())) {
                    //atentar ao fato de que a solução falha se o nome do
                    // vmm
                    // for alterado e não atualizado na tabela das vms
                    //To do: corrigir problema futuramente
                    VM.addVMM((CS_VMM) aux);
                    //adicionando VM para o VMM

                    final CS_VMM vmm = (CS_VMM) aux;
                    vmm.addVM(VM);

                }

            }
            this.virtualMachines.add(VM);
        }
    }

    public RedeDeFilasCloud build() {
        List<String> owners = new ArrayList<>(0);
        List<Double> powers = new ArrayList<>(0);

        for (final Map.Entry<String, Double> entry :
                this.powerLimits.entrySet()) {
            owners.add(entry.getKey());
            powers.add(entry.getValue());
        }
        //cria as métricas de usuarios para cada mestre
        for (final CS_Processamento mestre : this.virtualMachineMasters) {
            final CS_VMM mst = (CS_VMM) mestre;
            final MetricasUsuarios mu = new MetricasUsuarios();
            mu.addAllUsuarios(owners, powers);
            mst.getEscalonador().setMetricaUsuarios(mu);
        }
        final RedeDeFilasCloud rdf =
                new RedeDeFilasCloud(this.virtualMachineMasters,
                        this.machines, this.virtualMachines,
                        this.links,
                        this.internets);
        //cria as métricas de usuarios globais da rede de filas
        final MetricasUsuarios mu = new MetricasUsuarios();
        mu.addAllUsuarios(owners, powers);
        rdf.setUsuarios(owners);
        return rdf;
    }
}
