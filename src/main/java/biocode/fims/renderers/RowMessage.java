package biocode.fims.renderers;

/**
 * Handle messaging for
 */
public class RowMessage {
    public static final Integer WARNING = 0;
    public static final Integer ERROR = 1;
    protected String message;
    protected Integer level;
    protected   String groupMessage;
    private Integer row;

    public RowMessage(String message, String groupMessage, Integer level) {
        this.message = message;
        this.level = level;
        this.groupMessage = groupMessage;
    }

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

    public String getGroupMessageAsString() {
        return "<div id='groupMessage' class='" + getLevelAsString()+ "'>" + this.getLevelAsString() + ": " + groupMessage + "</div>";

    }

    /**
     * @return Message for this line
     */
    public String print() {
        if (this.row != null) {
            return "Row " + this.row + ": " + message;
        } else {
            return message;
        }

    }


    public void setRow(Integer row) {
        this.row = row;
    }


    public String getLevelAsString() {
        if (level == 0) return "Warning";
        else return "Error";
    }

    public Integer getLevel() {
        return level;
    }

    public   String getGroupMessage() {
        return groupMessage;
    }

    public String getMessage() {
        return message;
    }
}

