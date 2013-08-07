package com.plugtree.training;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashSet;

import org.drools.core.base.BaseEvaluator;
import org.drools.core.base.ValueType;
import org.drools.core.base.evaluators.EvaluatorCache;
import org.drools.core.base.evaluators.EvaluatorDefinition;
import org.drools.core.base.evaluators.Operator;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.rule.VariableRestriction.ObjectVariableContextEntry;
import org.drools.core.rule.VariableRestriction.VariableContextEntry;
import org.drools.core.spi.Evaluator;
import org.drools.core.spi.FieldValue;
import org.drools.core.spi.InternalReadAccessor;

public class DisjointEvaluatorDefinition implements EvaluatorDefinition {

	public static final Operator  DISJOINT     = Operator.addOperatorToRegistry( "disjoint", false );
    public static final Operator  NOT_DISJOINT = Operator.addOperatorToRegistry( "disjoint", true );
    
    private static final String[] SUPPORTED_IDS = { DISJOINT.getOperatorString() };
    
    private EvaluatorCache evaluators = new EvaluatorCache() {
        private static final long serialVersionUID  = 600l;
        {
            addEvaluator( ValueType.OBJECT_TYPE, DISJOINT,     DisjointEvaluator.INSTANCE );
            addEvaluator( ValueType.OBJECT_TYPE, NOT_DISJOINT, NotDisjointEvaluator.INSTANCE );
        }
    };
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject( evaluators );
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.evaluators = (EvaluatorCache) in.readObject();
	}

	@Override
	public String[] getEvaluatorIds() {
		return SUPPORTED_IDS;
	}

	@Override
	public boolean isNegatable() {
		return true;
	}

	@Override
	public Evaluator getEvaluator(ValueType type, String operatorId,
			boolean isNegated, String parameterText, Target leftTarget,
			Target rightTarget) {
		return this.evaluators.getEvaluator(type, Operator.determineOperator(operatorId, isNegated));
	}

	@Override
	public Evaluator getEvaluator(ValueType type, String operatorId,
			boolean isNegated, String parameterText) {
		return this.getEvaluator(type, operatorId, isNegated, parameterText, Target.FACT, Target.FACT);
	}

	@Override
	public Evaluator getEvaluator(ValueType type, Operator operator,
			String parameterText) {
		return this.evaluators.getEvaluator(type, operator);
	}

	@Override
	public Evaluator getEvaluator(ValueType type, Operator operator) {
		return this.evaluators.getEvaluator(type, operator);
	}

	@Override
	public boolean supportsType(ValueType type) {
		return this.evaluators.supportsType(type);
	}

	@Override
	public Target getTarget() {
		return Target.FACT;
	}

	public static class DisjointEvaluator extends BaseEvaluator {
	        
		private static boolean isIntersectionEmpty( Collection<?> a, Collection<?> b ){
			if( a == null || b == null ) return false;
			Collection<?> h = new HashSet<Object>( a );
			h.retainAll( b );
			return h.isEmpty();
		}

		private static final long     serialVersionUID = 510l;
		public final static Evaluator INSTANCE         = new DisjointEvaluator();
		 
		public DisjointEvaluator() {
			super( ValueType.OBJECT_TYPE, DISJOINT );
		}

		@Override
		public boolean evaluate(InternalWorkingMemory workingMemory,
				InternalReadAccessor extractor, InternalFactHandle factHandle,
				FieldValue value) {
			final Collection<?> set2 = (Collection<?>) value.getValue();
			final Collection<?> set1 = (Collection<?>) extractor.getValue( workingMemory, factHandle );
			return isIntersectionEmpty( set1, set2 );
		}

		@Override
		public boolean evaluate(InternalWorkingMemory workingMemory,
				InternalReadAccessor leftExtractor, InternalFactHandle left,
				InternalReadAccessor rightExtractor, InternalFactHandle right) {
			 final Collection<?> set2 = (Collection<?>)leftExtractor.getValue( workingMemory, left );
			 final Collection<?> set1 = (Collection<?>) rightExtractor.getValue( workingMemory, right );
			 return isIntersectionEmpty( set1, set2 );
		}

		@Override
		public boolean evaluateCachedLeft(InternalWorkingMemory workingMemory,
				VariableContextEntry context, InternalFactHandle right) {
			final Collection<?> set2 = (Collection<?>)((ObjectVariableContextEntry) context).left;
			InternalFactHandle rightHandle = (InternalFactHandle) context.extractor.getValue( workingMemory, right );
			final Collection<?> set1 = (Collection<?>) rightHandle.getObject();
			return isIntersectionEmpty( set1, set2 );
		}

		@Override
		public boolean evaluateCachedRight(InternalWorkingMemory workingMemory,
				VariableContextEntry context, InternalFactHandle left) {
			 final Collection<?> set2 = (Collection<?>) context.declaration.getExtractor().getValue( workingMemory, left );
			 final Collection<?> set1 = (Collection<?>) ((ObjectVariableContextEntry) context).right;
	         return isIntersectionEmpty( set1, set2 );
		}

		@Override
		public String toString() {
			return "Set disjoint";
		}
	}
	 
	 
	 
	 
	 
	 
	 
	 
	public static class NotDisjointEvaluator extends BaseEvaluator {
	        
		private static boolean isIntersectionNotEmpty( Collection<?> a, Collection<?> b ){
			if( a == null || b == null ) return false;
			Collection<?> h = new HashSet<Object>( a );
			h.retainAll( b );
			return ! h.isEmpty();
		}

		private static final long     serialVersionUID = 510l;
		public final static Evaluator INSTANCE         = new NotDisjointEvaluator();
		 
		public NotDisjointEvaluator() {
			super( ValueType.OBJECT_TYPE, NOT_DISJOINT );
		}

		@Override
		public boolean evaluate(InternalWorkingMemory workingMemory,
				InternalReadAccessor extractor, InternalFactHandle factHandle,
				FieldValue value) {
			final Collection<?> set2 = (Collection<?>) value.getValue();
			final Collection<?> set1 = (Collection<?>) extractor.getValue( workingMemory, factHandle );
			return isIntersectionNotEmpty( set1, set2 );
		}

		@Override
		public boolean evaluate(InternalWorkingMemory workingMemory,
				InternalReadAccessor leftExtractor, InternalFactHandle left,
				InternalReadAccessor rightExtractor, InternalFactHandle right) {
			 final Collection<?> set2 = (Collection<?>)leftExtractor.getValue( workingMemory, left );
			 final Collection<?> set1 = (Collection<?>) rightExtractor.getValue( workingMemory, right );
			 return isIntersectionNotEmpty( set1, set2 );
		}

		@Override
		public boolean evaluateCachedLeft(InternalWorkingMemory workingMemory,
				VariableContextEntry context, InternalFactHandle right) {
			final Collection<?> set2 = (Collection<?>)((ObjectVariableContextEntry) context).left;
			InternalFactHandle rightHandle = (InternalFactHandle) context.extractor.getValue( workingMemory, right );
			final Collection<?> set1 = (Collection<?>) rightHandle.getObject();
			return isIntersectionNotEmpty( set1, set2 );
		}

		@Override
		public boolean evaluateCachedRight(InternalWorkingMemory workingMemory,
				VariableContextEntry context, InternalFactHandle left) {
			 final Collection<?> set2 = (Collection<?>) context.declaration.getExtractor().getValue( workingMemory, left );
			 final Collection<?> set1 = (Collection<?>) ((ObjectVariableContextEntry) context).right;
	         return isIntersectionNotEmpty( set1, set2 );
		}

		@Override
		public String toString() {
			return "Set not disjoint";
		}
	}
}
