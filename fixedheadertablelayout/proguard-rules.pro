# Consumer ProGuard rules for FixedHeaderTableLayout.
#
# These rules ship inside the AAR and are merged into the consumer app's R8 config
# when the app sets `minifyEnabled true`. Their job: keep the library's public API
# surface so consumer code that references it survives shrinking.
#
# Internal types (anything under `internal/`) are intentionally not kept here — they
# are reachable from the public API through reference, which is enough for R8 to
# preserve what it actually needs.

-keep public class io.github.jbailey2010.fixedheadertable.FixedHeaderTable { *; }
-keep public class io.github.jbailey2010.fixedheadertable.FixedHeaderTableAdapter { *; }
-keep public class io.github.jbailey2010.fixedheadertable.CellViewHolder { *; }
-keep public class io.github.jbailey2010.fixedheadertable.SharedColumnWidths { *; }
-keep public class io.github.jbailey2010.fixedheadertable.SharedColumnWidths$Listener { *; }

# The view's saved-state Parcelable must keep its CREATOR field for instance-state
# restoration to find it via reflection.
-keepclassmembers class io.github.jbailey2010.fixedheadertable.FixedHeaderTable$SavedState {
    public static ** CREATOR;
}
