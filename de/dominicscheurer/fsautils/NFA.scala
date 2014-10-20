package de.dominicscheurer.fsautils {
	import Types._
	import Conversions._
	import Helpers._
	import FSAMethods._

	class NFA(
	    var alphabet: Set[Letter],
	    var states: Set[State],
	    var initialState: State,
	    var delta: ((State, Letter) => Option[Set[State]]),
	    var accepting: Set[State]) {
	  
	  require(states contains initialState)
	  require(accepting subsetOf states)
	  
	  // TODO (for contatenization)
//	  def getRenamedCopy: NFA = {
//	    var renameMap : Map[State, State] = null
//	    renameMap = states.foldLeft(renameMap.empty){ (z,s) =>
//	      	z + (s -> s)
//	      }
//	    
//	    (alphabet, statesRen, initialStateRen, deltaRen _, acceptingRen)
//	  }
	  
	  def accepts(word: String): Boolean =
	    accepts(for (x <- word.toList) yield Symbol(x toString))
	  
	  def accepts(word: Word): Boolean = accepts(word, initialState)
	  
	  def accepts(word: Word, fromState: State): Boolean = word match {
	    case Nil => accepting contains fromState
	    case letter :: rest =>
	      delta(fromState, letter) match {
	        case None => false
	        case Some(listOfStates) =>
	          listOfStates.foldLeft(false)(
	            (result: Boolean, possibleSuccessor: State) =>
	              result || accepts(rest, possibleSuccessor))
	      }
	  }
	  
	  def * : NFA = {
	    def deltaStar (state: State, letter: Letter) : Option[Set[State]] =
		  delta (state, letter) match {
	      	case None => None
	      	case Some(setOfStates) =>
	      	  Some(setOfStates ++ setOfStates.filter(accepting contains _)
	      	      .foldLeft(Set(initialState))((_,_) => Set(initialState)))
	      }
	    
	    (alphabet, states, initialState, deltaStar _, accepting)
	  }
	  
	  def ++(other: NFA): NFA = {
	    //TODO: Treat case that this automaton accepts the empty word
	    
	    def statesCup = states ++ other.states // TODO: Ensure that states are disjunct
	    
	    def deltaCup (state: State, letter: Letter) : Option[Set[State]] =
		  if (other.states contains state)
		    other.delta (state, letter) // Delta_2
		  else // Delta_1 \cup Delta_{1->2}
		    delta (state, letter) match {
		      	case None => None
		      	case Some(setOfStates) =>
		      	  Some(setOfStates ++ setOfStates.filter(accepting contains _)
		      	      .foldLeft(Set(other.initialState))((_,_) => Set(other.initialState)))
		      }
	    
	    (alphabet, statesCup, initialState, deltaCup _, other.accepting)
	  }
	  
	  def toDFA : DFA = {
	    val pStates = powerSet(states).map(setOfStates => set(setOfStates)) : States
	    val pInitialState = set(Set(initialState)) : State
	    val pAccepting = pStates.filter{
	      case (set(setOfStates)) => (setOfStates intersect accepting) nonEmpty
	      case _ => error("Impossible case")
	    }
	    
	    def pDelta (state: State, letter: Letter) =
		  (state, letter) match {
	      	case (set(setOfStates), letter) =>
	      	  set(setOfStates.foldLeft(Set(): States)((result, q) =>
	      	    delta(q, letter) match {
	      	      case None => result
	      	      case Some(setOfTargetStates) => result union setOfTargetStates
	      	    }))
	        case _ => error("Impossible case")
	      }
	    
	    (alphabet, pStates, pInitialState, pDelta _, pAccepting)
	  }
	  
	  override def toString = {
	    val indentSpace = "    "
	    val indentBeginner = "|"
	    val indent = "|" + indentSpace
	    val dindent = indent + indentSpace
	    var sb = new StringBuilder()
	    
	    sb ++= "NFA (Z,S,q0,D,A) with\n"
	    
	    sb ++= toStringUpToDelta(
	        indentBeginner,
	        indentSpace,
	        "Z", alphabet,
	        "S", states,
	        "q0", initialState,
	        "A", accepting);
	      
	    sb ++= indent ++= "D = {"
	    states.foreach(s =>
	      alphabet.filter(l => !delta(s,l).isEmpty).foreach(l =>
	        sb ++= "\n"
	           ++= dindent
	           ++= "("
	             ++= s.toString
	             ++= ","
	             ++= l.name
	             ++= ") => "
	             ++= (delta(s,l) match {
	             	   case None => "{}"
	             	   case Some(setOfStates) =>
	             	     setOfStates.foldLeft("")(
	             	         (result, aState) => result + aState.toString + " | ").stripSuffix(" | ")
	        	     })
	           ++= ","))
	    sb = sb.dropRight(1 - alphabet.isEmpty)
	    sb ++= "\n" ++= indent ++= "}\n"
	      
	    sb toString
	  }
		
	}
}