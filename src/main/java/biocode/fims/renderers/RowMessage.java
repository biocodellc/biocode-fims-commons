package biocode.fims.renderers;

/**
 * Handle messaging for
 */
public class RowMessage implements Message {
    public static final Integer WARNING = 0;
    public static final Integer ERROR = 1;
    protected String message;
    protected Integer level;
    protected   String groupMessage;
    private Integer row;

    public RowMessage(String msg, int row) {
        this.message = msg;
        this.row = row;
    }

    @Override
    public String message() {
        return "Row " +
                row +
                ": " +
                this.message;
    }

    @Deprecated
    public RowMessage(String message, String groupMessage, Integer level) {
        this.message = message;
        this.level = level;
        this.groupMessage = groupMessage;
    }

    @Deprecated
    public RowMessage(String message, String groupMessage, Integer level, Integer row) {
        this.message = message;
        this.row = row;
        if (groupMessage == null) {
            this.groupMessage = this.level + ": Unclassified message";
        } else {
            this.groupMessage = groupMessage;
        }

        this.level = level;
    }

    @Deprecated
    public String getGroupMessageAsString() {
        return "<div id='groupMessage' class='" + getLevelAsString()+ "'>" + this.getLevelAsString() + ": " + groupMessage + "</div>";

    }

    /**
     * @return Message for this line
     */
    @Deprecated
    public String print() {
        if (this.row != null) {
            return "Row " + this.row + ": " + message;
        } else {
            return message;
        }

    }


    private String getLevelAsString() {
        if (level == 0) return "Warning";
        else return "Error";
    }

    @Deprecated
    public Integer getLevel() {
        return level;
    }

    @Deprecated
    public String getGroupMessage() {
        return groupMessage;
    }
}

