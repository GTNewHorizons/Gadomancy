package makeo.gadomancy.common.utils;

import com.gtnewhorizons.aspectrecipeindex.api.IAspectRangeList;

/**
 * Variant of {@link RandomizedAspectList} that also implements Aspect Recipe Index's range api, so NEI can show the
 * possible cost ranges. Only instantiated reflectively from RegisteredRecipes, because loading this class requires an
 * Aspect Recipe Index version providing {@link IAspectRangeList}; Gadomancy itself has to keep working without it.
 */
public class RandomizedAspectListRanged extends RandomizedAspectList implements IAspectRangeList {
}
