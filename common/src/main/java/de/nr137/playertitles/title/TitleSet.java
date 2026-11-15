package de.nr137.playertitles.title;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** One title file: a set of titles, which can be switched off as a whole ({@code "enabled": false}). */
public record TitleSet(String name, @Nullable Boolean enabled, List<Title> titles) {
}
