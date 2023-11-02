package fr.inria.astor.core.solutionsearch.spaces.ingredients.scopes;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.martiansoftware.jsap.JSAPException;

import fr.inria.astor.core.entities.Ingredient;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.filters.TargetElementProcessor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;

/**
 * 
 * @author Matias Martinez, matias.martinez@inria.fr
 * 
 */
public class GlobalBasicIngredientSpace extends LocalIngredientSpace {

	private Logger logger = Logger.getLogger(GlobalBasicIngredientSpace.class.getName());

	public GlobalBasicIngredientSpace(TargetElementProcessor<?> processor) throws JSAPException {
		super(processor);

	}

	public GlobalBasicIngredientSpace(List<TargetElementProcessor<?>> processor) throws JSAPException {
		super(processor);

	}

	/**
	 * Ignored the class, returns all elements from the space
	 */
	@Override
	public List<Ingredient> getIngredients(CtElement element) {
		List result = new ArrayList();
		for (String type : fixSpaceByType.keySet()) {
			result.addAll(this.fixSpaceByType.get(type));
		}

		return result;
	}

	/**
	 * Ignores the element, returns all types
	 */
	@Override
	public List<Ingredient> getIngredients(CtElement element, String type) {

		return this.fixSpaceByType.get(type);

	}

	@Override
	public IngredientPoolScope spaceScope() {
		return IngredientPoolScope.GLOBAL;
	}

	public String toString() {
		String s = "--Space: " + this.spaceScope() + "\n";
		int totalIng = 0;
		for (String l : this.fixSpaceByType.keySet()) {
			List ing = this.fixSpaceByType.get(l);
			s += "\t " + l + ": (" + ing.size() + ") " + /* ing + */ "\n";
			totalIng += ing.size();
		}
		s = " All ingredients: " + totalIng + s;
		return s;
	}

	@Override
	public void defineSpace(ProgramVariant variant) {
		List<CtType<?>> affected = MutationSupporter.getFactory().Type().getAll();
		for (CtType<?> classToProcess : affected) {
			this.createFixSpaceFromAClass(classToProcess);
		}

	}

}
