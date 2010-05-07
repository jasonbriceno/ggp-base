package util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Proposition;
import util.propnet.factory.CachedPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.query.ProverQueryBuilder;

@SuppressWarnings("unused")
public class PropNetStateMachine extends StateMachine {
	/**
	 * Computes if the state is terminal.  Should return the value of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		//TODO compute if the MachineState is terminal
		return false;
	}
	
	/**
	 * Computes the goal for a role in the current state. Should return the value of the goal proposition that is true for 
	 * role.  If the number of goals that are true for role != 1, then you should throw a GoalDefinitionException
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		//TODO compute the goal for role in state
		return -1;
	}
	
	/**
	 * Returns the initial state.  The initial state can be computed by only setting the truth value of the init 
	 * proposition to true, and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		//TODO compute the initial state
		return null;
	}
	
	/**
	 * Computes the legal moves for role in state
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		//TODO compute legal moves
		return null;
	}
	
	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		//TODO compute the next state
		return null;
	}
	
	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 * The base propositions and input propositions should always be exempt from this ordering
	 * The base propositions values are set from the MachineState that operations are performed on and the
	 * input propositions are set from the Moves that operations are performed on as well (if any).
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
		//TODO compute the topological ordering
		List<Proposition> order = new LinkedList<Proposition>();
		//All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(pnet.getComponents());
		//All of the propositions in the prop net
		List<Proposition> propositions = new ArrayList<Proposition>(pnet.getPropositions());
		return order;
	}
	
	/** Already implemented for you */
	@Override
	public Move getMoveFromSentence(GdlSentence sentence) {
		return new PropNetMove(sentence);
	}
	
	/** Already implemented for you */
	@Override
	public MachineState getMachineStateFromSentenceList(
			Set<GdlSentence> sentenceList) {
		return new PropNetMachineState(sentenceList);
	}
	
	/** Already implemented for you */
	@Override
	public Role getRoleFromProp(GdlProposition proposition) {
		return new PropNetRole(proposition);
	}
	
	/** Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/** The underlying proposition network  */
	private PropNet pnet;
	/** An index from GdlTerms to Base Propositions.  The truth value of base propositions determines the state */
	private Map<GdlTerm, Proposition> basePropositions;
	/** An index from GdlTerms to Input Proposiitons.  Input propositions correspond to moves a player can take */
	private Map<GdlTerm, Proposition> inputPropositions;
	/** The terminal proposition.  If the terminal proposition's value is true, the game is over */
	private Proposition terminal;
	/** Maps roles to their legal propositions */
	private Map<Role, Set<Proposition>> legalPropositions;
	/** Maps roles to their goal propositions */
	private Map<Role, Set<Proposition>> goalPropositions;
	/** The topological ordering of the propositions */
	private List<Proposition> ordering;
	/** Set to true and everything else false, then propagate the truth values to compute the initial state*/
	private Proposition init;
	/** The roles of different players in the game */
	private List<Role> roles;
	/** A map between legal and input propositions.  The map contains mappings in both directions*/
	private Map<Proposition, Proposition> legalInputMap;

	/**
	 * Initializes the PropNetStateMachine.  You should compute the topological ordering here.
	 * Additionally you can compute the initial state here, if you want.
	 */
	@Override
	public void initialize(List<Gdl> description) {
		pnet = CachedPropNetFactory.create(description);
		roles = computeRoles(description);
		basePropositions = pnet.getBasePropositions();
		inputPropositions = pnet.getInputPropositions();
		terminal = pnet.getTerminalProposition();
		legalPropositions = pnet.getLegalPropositions();
		init = pnet.getInitProposition();
		goalPropositions = pnet.getGoalPropositions();
		legalInputMap = pnet.getLegalInputMap();
		ordering = getOrdering();
	}

/*Helper methods*/
	/**
	 * The Input propositions are indexed by (does ?player ?action)
	 * This translates a List of Moves (backed by a sentence that is simply ?action)
	 * to GdlTerms that can be used to get Propositions from inputPropositions
	 *  and accordingly set their values etc.  This is a naive implementation when coupled with 
	 *  setting input values, feel free to change this for a more efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlTerm> toDoes(List<Move> moves)
	{
		List<GdlTerm> doeses = new ArrayList<GdlTerm>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();
		
		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)).toTerm());
		}
		return doeses;
	}
	
	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	
	public static Move getMoveFromProposition(Proposition p)
	{
		return new PropNetMove(p.getName().toSentence().get(1).toSentence());
	}
	
	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */	
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}
	
	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */	
	public PropNetMachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : basePropositions.values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName().toSentence());
			}

		}
		return new PropNetMachineState(contents);
	}

	/**
	 * Helper method, used to get compute roles.  You should only be using this
	 * for Role indexing (because of compatibility with the GameServer state machine's roles)
	 * @param description
	 * @return the list of roles for the current game.  Compatible with the GameServer's state machine.
	 */
	private List<Role> computeRoles(List<Gdl> description)
	{
		List<Role> roles = new ArrayList<Role>();
		for (Gdl gdl : description)
		{
			if (gdl instanceof GdlRelation)
			{
				GdlRelation relation = (GdlRelation) gdl;				
				if (relation.getName().getValue().equals("role"))
				{
					roles.add(new PropNetRole((GdlProposition) relation.get(0).toSentence()));
				}
			}
		}
		return roles;
	}
}