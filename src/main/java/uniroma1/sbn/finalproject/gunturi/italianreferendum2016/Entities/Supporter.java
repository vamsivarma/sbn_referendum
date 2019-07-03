package uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Entities;

/**
 * This entity represents the supporter, someone who mentioned politicians, used structures 
 * discovered during the analisys and/or use yes and/or no well known expression 
 * @author Vamsi Gunturi
 */
public class Supporter {
    // User twitter id
    private String id;
    // User twitter screen name
    private String name;
    // Number of yes politicians mentioned
    private int yesPolsMentioned;
    // Number of no politicians mentioned
    private int noPolsMentioned;
    // Number of yes constructions mentioned
    private int yesCostructionsUsed;
    // Number of no constructions mentioned
    private int noCostructionsUsed;
    // Number of yes expressions mentioned
    private int yesExpressionsUsed;
    // Number of no expressions mentioned
    private int noExpressionsUsed;
    // Is this supporter a Yes politician?
    private Boolean isAYesPol;
    // Is this supporter a No politician?
    private Boolean isANoPol;

    /**
     * Initialize user id and screen name
     * @param id user id
     * @param name screen name
     */
    public Supporter(String id, String name) {
        this.id = id;
        this.name = name;
        // Initial value of the other attributes
        this.yesPolsMentioned = 0;
        this.noPolsMentioned = 0;
        this.yesCostructionsUsed = 0;
        this.noCostructionsUsed = 0;
        this.yesExpressionsUsed = 0;
        this.noExpressionsUsed = 0;
        this.isAYesPol = Boolean.FALSE;
        this.isANoPol = Boolean.FALSE;
    }

    /**
     *
     * @return user id
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return the number of yes politicians mentioned
     */
    public int getYesPolsMentioned() {
        return yesPolsMentioned;
    }

    /**
     *
     * @return the number of no politicians mentioned
     */
    public int getNoPolsMentioned() {
        return noPolsMentioned;
    }

    /**
     *
     * @return the number of yes costructions used
     */
    public int getYesCostructionsUsed() {
        return yesCostructionsUsed;
    }

    /**
     *
     * @return the number of no costructions used
     */
    public int getNoCostructionsUsed() {
        return noCostructionsUsed;
    }

    /**
     *
     * @return the number of yes expresions used
     */
    public int getYesExpressionsUsed() {
        return yesExpressionsUsed;
    }

    /**
     *
     * @return the number of no expresions used
     */
    public int getNoExpressionsUsed() {
        return noExpressionsUsed;
    }

    /**
     *
     * @return the value of the bool value
     */
    public Boolean getIsAYesPol() {
        return isAYesPol;
    }

    /**
     *
     * @return the value of the bool value
     */
    public Boolean getIsANoPol() {
        return isANoPol;
    }

    /**
     *
     * @return the screen name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param yesPolsMentioned
     */
    public void setYesPolsMentioned(int yesPolsMentioned) {
        this.yesPolsMentioned = yesPolsMentioned;
    }

    /**
     *
     * @param noPolsMentioned
     */
    public void setNoPolsMentioned(int noPolsMentioned) {
        this.noPolsMentioned = noPolsMentioned;
    }

    /**
     *
     * @param yesCostructionsUsed
     */
    public void setYesCostructionsUsed(int yesCostructionsUsed) {
        this.yesCostructionsUsed = yesCostructionsUsed;
    }

    /**
     *
     * @param noCostructionsUsed
     */
    public void setNoCostructionsUsed(int noCostructionsUsed) {
        this.noCostructionsUsed = noCostructionsUsed;
    }

    /**
     *
     * @param yesExpressionsUsed
     */
    public void setYesExpressionsUsed(int yesExpressionsUsed) {
        this.yesExpressionsUsed = yesExpressionsUsed;
    }

    /**
     *
     * @param noExpressionsUsed
     */
    public void setNoExpressionsUsed(int noExpressionsUsed) {
        this.noExpressionsUsed = noExpressionsUsed;
    }

    /**
     *
     * @param isAYesPol
     */
    public void setIsAYesPol(Boolean isAYesPol) {
        this.isAYesPol = isAYesPol;
    }

    /**
     *
     * @param isANoPol
     */
    public void setIsANoPol(Boolean isANoPol) {
        this.isANoPol = isANoPol;
    }
    
    /**
     *
     * @return
     */
    public String toString(){
        return (name + " " + id + " " + isANoPol + " " + isAYesPol + " " + noCostructionsUsed + " " + 
                yesCostructionsUsed + " " + noExpressionsUsed + " " + yesExpressionsUsed + " " + 
                noPolsMentioned + " " + yesPolsMentioned);
    }
}
