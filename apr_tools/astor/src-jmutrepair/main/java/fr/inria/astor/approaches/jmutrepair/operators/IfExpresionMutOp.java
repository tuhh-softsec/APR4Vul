package fr.inria.astor.approaches.jmutrepair.operators;

import java.util.List;

import fr.inria.astor.approaches.jmutrepair.MutantCtElement;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import spoon.reflect.code.CtIf;
import spoon.reflect.declaration.CtElement;

/**
 * 
 * @author Matias Martinez
 *
 */
@SuppressWarnings("rawtypes")
public class IfExpresionMutOp extends ExpresionMutOp {

	public IfExpresionMutOp() {
		super();
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		return (point.getCodeElement() instanceof CtIf);
	}

	protected OperatorInstance createModificationInstance(ModificationPoint point, MutantCtElement fix)
			throws IllegalAccessException {
		CtIf targetIF = (CtIf) point.getCodeElement();
		OperatorInstance operation = new OperatorInstance();
		operation.setOriginal(targetIF.getCondition());
		operation.setOperationApplied(this);
		operation.setModificationPoint(point);
		operation.setModified(fix.getElement());

		return operation;
	}

	/** Return the list of CtElements Mutanted */
	@Override
	public List<MutantCtElement> getMutants(CtElement element) {

		CtIf targetIF = (CtIf) element;
		List<MutantCtElement> mutations = null;
		mutations = this.mutatorBinary.execute(targetIF.getCondition());
		return mutations;
	}

	@Override
	public boolean updateProgramVariant(OperatorInstance opInstance, ProgramVariant p) {
		// TODO Auto-generated method stub
		return false;
	}

}
