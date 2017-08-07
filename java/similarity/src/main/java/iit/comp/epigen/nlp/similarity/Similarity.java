package iit.comp.epigen.nlp.similarity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.openrdf.model.URI;

import slib.graph.algo.extraction.validator.dag.ValidatorDAG;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.utils.GraphActionExecutor;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Corpus;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;
import slib.utils.ex.SLIB_Ex_Critic;
import slib.utils.ex.SLIB_Exception;


public class Similarity {
	/**
	 * Class used to compute semantic similarity measures between pairs of concepts
	 * and group of concepts from a given ontology
	 *
	 * @author Eugenia Galeota (eugenia.galeota@iit.it)
	 *
	 */
//	Logger logger = LoggerFactory.getLogger(this.getClass());


	/**
	 *
	 * @param ontologyPath : the path of the ontology file to be loaded (either OBO or RDF_XML)
	 * @return instance of a graph
	 * @throws SLIB_Exception
	 */
	public G loadOntology(String ontologyPath ) throws SLIB_Exception {
		GDataConf dataConf;
		URIFactory factory = URIFactoryMemory.getSingleton();
		URI graphURI = factory.getURI("http://graph/");
		G g = new GraphMemory(graphURI);
		if("obo".equals(FilenameUtils.getExtension(ontologyPath)))
			dataConf = new GDataConf(GFormat.OBO, ontologyPath);
		else
			dataConf = new GDataConf(GFormat.RDF_XML, ontologyPath);

		GraphConf gConf = new GraphConf();
		gConf.addGDataConf(dataConf);
		GraphLoaderGeneric.populate(dataConf, g);

		//Redefining some types of part_of and develops_from to is_a relations

//		logger.info("Changing part_of and develops_from to is_a relations");
		GAction predicateSubstitute = new GAction(GActionType.PREDICATE_SUBSTITUTE);

		predicateSubstitute.addParameter("old_uri", "http://graph/part_of");
		predicateSubstitute.addParameter("new_uri", "RDFS.SUBCLASSOF");
		GraphActionExecutor.applyAction(predicateSubstitute, g);

		predicateSubstitute = new GAction(GActionType.PREDICATE_SUBSTITUTE);
		predicateSubstitute.addParameter("old_uri", "http://graph/develops_from");
		predicateSubstitute.addParameter("new_uri", "RDFS.SUBCLASSOF");
		GraphActionExecutor.applyAction(predicateSubstitute, g);

		// Creating a unique root if there is more then one

		Set<URI> roots = new ValidatorDAG().getTaxonomicRoots(g);
		if(roots.size() > 1){
			GAction actionRerootConf = new GAction(GActionType.REROOTING);
			GraphActionExecutor.applyAction(actionRerootConf, g);
		}


	//	logger.info(g.toString());
		return(g);
	}


	/* Setting the configuration for pairwise comparisons */
	/**
	 *
	 * @param pairwiseShortFlag
	 * @param icConfShortFlag
	 * @return the configuration to compute pairwise semantic similarity
	 * @throws SLIB_Ex_Critic
	 */

	public SMconf setPairwiseConfig(String pairwiseShortFlag, String icConfShortFlag) throws SLIB_Ex_Critic{

		String pairwise = SMConstants.SIM_PAIRWISE_SHORT_FLAG.get(pairwiseShortFlag);
		SMconf config = new SMconf(pairwise);
		if(!pairwise.contains("_EDGE")){
			ICconf icConf = null;
			if(SMConstants.IC_SHORT_FLAG.get(icConfShortFlag)!=null){
				if(!SMConstants.IC_SHORT_FLAG.get(icConfShortFlag).contains("_annot"))
					icConf = new IC_Conf_Topo(SMConstants.IC_SHORT_FLAG.get(icConfShortFlag));
				else
					icConf = new IC_Conf_Corpus(SMConstants.IC_SHORT_FLAG.get(icConfShortFlag));
				config.setICconf(icConf);
			}
			else{
//				logger.info("Setting IC to default value Resnik. Please run show Options method to view valid IC values");
				icConf = new IC_Conf_Topo(SMConstants.IC_SHORT_FLAG.get("resnik"));
			}
		}

		return config;
	}


	/**
	 *
	 * @param groupwiseShortFlag
	 * @return the configuration to compute groupwise semantic similarity
	 * @throws SLIB_Ex_Critic
	 */

	public SMconf setGroupwiseConfig(String groupwiseShortFlag) throws SLIB_Ex_Critic{
		String groupwise = SMConstants.SIM_GROUPWISE_SHORT_FLAG.get(groupwiseShortFlag);
		SMconf config = new SMconf(groupwise);
		return config;
	}




	public ArrayList<Set<String>> showMeasures() throws SLIB_Ex_Critic{
		Set<String> pairwise = SMConstants.SIM_PAIRWISE_SHORT_FLAG.keySet();
		Set<String> infoContent = SMConstants.IC_SHORT_FLAG.keySet();
		Set<String> groupWiseIt = SMConstants.SIM_GROUPWISE_SHORT_FLAG.keySet();
		ArrayList<Set<String>> optionsList = new ArrayList<Set<String>>();
		optionsList.add(pairwise);
		optionsList.add(infoContent);
		optionsList.add(groupWiseIt);
		return optionsList;
	}






	/**
	 *
	 * @return HashMap with all the groupwise configurations
	 * @throws SLIB_Ex_Critic
	 */



	public Map<String, SMconf> getAllGroupwiseMeasures() throws SLIB_Ex_Critic{
		Iterator groupwiseIt = SMConstants.SIM_GROUPWISE_SHORT_FLAG.entrySet().iterator();
		Map<String, SMconf> configurations = new HashMap<String, SMconf>();
		while(groupwiseIt.hasNext()){
			Map.Entry pair = (Map.Entry) groupwiseIt.next();
			SMconf config = new SMconf(pair.getValue().toString());
			configurations.put(pair.getKey().toString(), config);
		}
		return configurations;
	}



	/**
	 *
	 * @return Hashmap with all the possible pairwise configurations
	 * @throws SLIB_Ex_Critic
	 */

	public Map<String, SMconf> getAllPairwiseCombinations() throws SLIB_Ex_Critic{
		Iterator pairwiseIt = SMConstants.SIM_PAIRWISE_SHORT_FLAG.entrySet().iterator();
		Map<String, SMconf> configurations = new HashMap<String, SMconf>();
		while(pairwiseIt.hasNext()){
			Map.Entry pair = (Map.Entry) pairwiseIt.next();
			SMconf config = new SMconf(pair.getValue().toString());
			if(!pair.getValue().toString().contains("_EDGE")){

				Iterator infoContent = SMConstants.IC_SHORT_FLAG.entrySet().iterator();
				while(infoContent.hasNext()){
					Map.Entry icPair = (Map.Entry) infoContent.next();
					ICconf icConf = null;
					String confStringIC = icPair.getValue().toString();

					if(!confStringIC.contains("_annot"))
						icConf = new IC_Conf_Topo(SMConstants.IC_SHORT_FLAG.get(icPair.getKey()));
					else
						icConf = new IC_Conf_Corpus(SMConstants.IC_SHORT_FLAG.get(icPair.getKey()));
					if(icConf!=null){
						config.setICconf(icConf);
						configurations.put(pair.getValue().toString() + "___________" + confStringIC, config);
					}
				}
			}else{
				configurations.put(pair.getValue().toString(), config);
			}
		}
		return configurations;
	}

	/** Computation of the groupwise similarity between sets of URIS belonging to a given ontology
	 *
	 * @param firstURIset the set of URIs in the first group
	 * @param secondURIset the set of URIs in the second group
	 * @param graph the graph object
	 * @param groupConfig the gropuwise configuration option
	 * @param pairConfig the pairwise configuration option
	 * @return
	 * @throws SLIB_Ex_Critic
	 */
	public double group_similarity(Set<URI> firstURIset, Set<URI> secondURIset, G graph, SMconf groupConfig, SMconf pairConfig) throws SLIB_Ex_Critic{

		double score = 0;
		SM_Engine engine;
		if(groupConfig.getId().equals(SMConstants.FLAG_SIM_GROUPWISE_BMA) ||
				groupConfig.getId().equals(SMConstants.FLAG_SIM_GROUPWISE_MIN) ||
				groupConfig.getId().equals(SMConstants.FLAG_SIM_GROUPWISE_AVERAGE) ||
				groupConfig.getId().equals(SMConstants.FLAG_SIM_GROUPWISE_MAX) ||
				groupConfig.getId().equals(SMConstants.FLAG_SIM_GROUPWISE_BMA) ||
				groupConfig.getId().equals(SMConstants.FLAG_SIM_GROUPWISE_BMM) ){
			try {
				engine = new SM_Engine(graph);
				score = engine.compare(groupConfig, pairConfig, firstURIset, secondURIset);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				engine = new SM_Engine(graph);
				score = engine.compare(groupConfig, firstURIset, secondURIset);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return score;

	}

	/**  Computation of the pairwise similarity between two URIs in a given graph
	 *
	 * @param firstURI
	 * @param secondURI
	 * @param graph
	 * @param pairConfig
	 * @return
	 * @throws SLIB_Ex_Critic
	 */
	public double pair_similarity(URI firstURI, URI secondURI, G graph, SMconf pairConfig) throws SLIB_Ex_Critic{
		SM_Engine engine;
		double score = 0;
		engine = new SM_Engine(graph);
		score = engine.compare(pairConfig, firstURI, secondURI);
		return score;
	}


	public Map<String, Double> pairWiseCompareAll(URI firstURI, URI secondURI, G graph) throws SLIB_Ex_Critic{
		Map<String, SMconf> configs = this.getAllPairwiseCombinations();
		Iterator it = configs.entrySet().iterator();
		Map<String, Double> results = new HashMap<String, Double>();
		while(it.hasNext()){
			Map.Entry conf_pair = (Map.Entry) it.next();
			double score = pair_similarity(firstURI, secondURI, graph,(SMconf)conf_pair.getValue() );
			results.put(conf_pair.getKey().toString(), new Double(score));
		}
		return results;
	}

	public Map<String, Double> groupWiseCompareAll(Set<URI> firstGroup, Set<URI> secondGroup, G graph) throws SLIB_Ex_Critic{
		Map<String, SMconf> groupConf = this.getAllGroupwiseMeasures();
		Map<String, SMconf> pairConf = this.getAllPairwiseCombinations();
		Map<String, Double> results = new HashMap<String, Double>();

		Iterator groupIt = groupConf.entrySet().iterator();
		while(groupIt.hasNext()){
			Map.Entry group_conf = (Map.Entry) groupIt.next();
			Iterator pairIt = groupConf.entrySet().iterator();
			while(pairIt.hasNext()){
				Map.Entry pair_conf = (Map.Entry) pairIt.next();
				double score = group_similarity(firstGroup, secondGroup, graph, (SMconf)group_conf.getValue(), (SMconf)pair_conf.getValue());
				results.put(group_conf.getKey().toString() + "_" + pair_conf.getKey().toString(), score);
			}
		}
		return results;

	}

	public Set<URI> createURIs(String [] terms){
		URIFactory factory = URIFactoryMemory.getSingleton();
		Set<URI> uriList = new HashSet<URI>();
		for(String term: terms){
			URI termURI = factory.getURI(term);
			uriList.add(termURI);
		}
		return uriList;
	}

	public URI createURI(String term){
		URIFactory factory = URIFactoryMemory.getSingleton();
		URI termURI = factory.getURI(term);
		return termURI;
	}

	public static void main(String [] args){

		Similarity sim = new Similarity();
		try {


			URIFactory factory = URIFactoryMemory.getSingleton();
      String filePath = "BrendaTissue.obo";
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      filePath = classLoader.getResource(filePath).getFile();
 			G graph = sim.loadOntology(filePath);
			System.out.println(graph.toString());
			System.out.println(graph.getE().toString());
			//	SMconf conf = sim.simil("topology", "resnik", "resnik");
			//	sim.printMap(SMConstants.pairwiseMeasureMapping);
			SM_Engine engine = new SM_Engine(graph);
			URI bto1 = factory.getURI("http://purl.obolibrary.org/obo/BTO_0004732");
			URI bto2 = factory.getURI("http://purl.obolibrary.org/obo/BTO_0000801");
			URI bto3 = factory.getURI("http://purl.obolibrary.org/obo/BTO_0000815");
			URI bto4 = factory.getURI("http://purl.obolibrary.org/obo/BTO_0001912");
			Set<URI> bto1e2 = new HashSet<URI>();
			bto1e2.add(bto1);
			bto1e2.add(bto2);

			Set<URI> bto3e4 = new HashSet<URI>();
			bto3e4.add(bto3);
			bto3e4.add(bto4);

			SMconf configMconf = sim.setPairwiseConfig("edge_li", null);
			System.out.println(configMconf.toString());
			double score = sim.pair_similarity(bto2,bto3, graph, configMconf );
			SMconf group = sim.setGroupwiseConfig("bma");
			double score2 = sim.group_similarity(bto1e2, bto3e4,  graph,group,  configMconf);
			System.out.println("Primo " + score +"\n Secondo " + score2);
		} catch (SLIB_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



}



