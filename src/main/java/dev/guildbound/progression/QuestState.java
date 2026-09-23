package dev.guildbound.progression;

/** One finite introductory contract chain per character; no repeatable rewards. */
public record QuestState(int completed, boolean active, int kills) {
    public QuestState {
        if (completed < 0 || completed > GuildContract.values().length || kills < 0
                || (!active && kills != 0) || (completed == GuildContract.values().length && active)
                || (active && kills > GuildContract.values()[completed].target))
            throw new IllegalArgumentException("Invalid quest state");
    }
    public static QuestState empty() { return new QuestState(0, false, 0); }
    public GuildContract contract() { return finished() ? null : GuildContract.values()[completed]; }
    public boolean finished() { return completed == GuildContract.values().length; }
    public boolean ready() { return active && kills == contract().target; }
    public QuestState accept() { return active || finished() ? this : new QuestState(completed, true, 0); }
    public QuestState kill(boolean matches) {
        return !active || ready() || !matches ? this : new QuestState(completed, true, kills + 1);
    }
    public QuestState claim() { return ready() ? new QuestState(completed + 1, false, 0) : this; }
}

