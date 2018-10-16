package androidx.core.app;

import androidx.fragment.app.Fragment;

/**
 * Created by Peter Hoc on 10/13/2018.
 */
public class BackstackAccessor {
    private BackstackAccessor() {
        throw new IllegalStateException("Not instantiatable");
    }

    public static boolean isFragmentOnBackStack(Fragment fragment) {
        return false;
    }
}
