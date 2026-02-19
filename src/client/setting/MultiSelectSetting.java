package client.setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MultiSelectSetting extends Setting<Set<String>>
{
    private final List<String> options;
    private final Map<String, String> canonicalByLower = new LinkedHashMap<String, String>();

    public MultiSelectSetting(String key, String name, String description, String[] options, String[] defaultSelected)
    {
        super(key, name, description, immutableSet(defaultSelected));
        this.options = this.buildOptions(options);
        this.set(this.getDefaultValue());
    }

    private List<String> buildOptions(String[] rawOptions)
    {
        if (rawOptions == null || rawOptions.length == 0)
        {
            return Collections.emptyList();
        }

        LinkedHashSet<String> out = new LinkedHashSet<String>();

        for (int i = 0; i < rawOptions.length; ++i)
        {
            String option = normalizeOption(rawOptions[i]);

            if (option == null)
            {
                continue;
            }

            out.add(option);
            this.canonicalByLower.put(option.toLowerCase(Locale.ROOT), option);
        }

        return Collections.unmodifiableList(new ArrayList<String>(out));
    }

    protected Set<String> normalize(Set<String> value)
    {
        if (this.options.isEmpty())
        {
            return Collections.emptySet();
        }

        Collection<String> source = value == null ? this.getDefaultValue() : value;
        LinkedHashSet<String> selected = new LinkedHashSet<String>();

        if (source != null)
        {
            for (String entry : source)
            {
                String option = this.resolveOption(entry);

                if (option != null)
                {
                    selected.add(option);
                }
            }
        }

        return Collections.unmodifiableSet(selected);
    }

    public List<String> getOptions()
    {
        return this.options;
    }

    public boolean isSelected(String option)
    {
        String resolved = this.resolveOption(option);
        return resolved != null && this.get().contains(resolved);
    }

    public void toggle(String option)
    {
        String resolved = this.resolveOption(option);

        if (resolved == null)
        {
            return;
        }

        LinkedHashSet<String> next = new LinkedHashSet<String>(this.get());

        if (!next.remove(resolved))
        {
            next.add(resolved);
        }

        this.set(next);
    }

    public void setSelected(String option, boolean selected)
    {
        String resolved = this.resolveOption(option);

        if (resolved == null)
        {
            return;
        }

        LinkedHashSet<String> next = new LinkedHashSet<String>(this.get());

        if (selected)
        {
            next.add(resolved);
        }
        else
        {
            next.remove(resolved);
        }

        this.set(next);
    }

    public void setSelections(Collection<String> values)
    {
        LinkedHashSet<String> next = new LinkedHashSet<String>();

        if (values != null)
        {
            for (String value : values)
            {
                String resolved = this.resolveOption(value);

                if (resolved != null)
                {
                    next.add(resolved);
                }
            }
        }

        this.set(next);
    }

    private String resolveOption(String option)
    {
        String normalized = normalizeOption(option);

        if (normalized == null)
        {
            return null;
        }

        String direct = this.canonicalByLower.get(normalized.toLowerCase(Locale.ROOT));
        return direct == null ? null : direct;
    }

    private static String normalizeOption(String option)
    {
        if (option == null)
        {
            return null;
        }

        String trimmed = option.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Set<String> immutableSet(String[] values)
    {
        if (values == null || values.length == 0)
        {
            return Collections.emptySet();
        }

        LinkedHashSet<String> out = new LinkedHashSet<String>();

        for (int i = 0; i < values.length; ++i)
        {
            String option = normalizeOption(values[i]);

            if (option != null)
            {
                out.add(option);
            }
        }

        return Collections.unmodifiableSet(out);
    }
}
