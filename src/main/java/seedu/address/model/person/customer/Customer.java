package seedu.address.model.person.customer;

import java.util.Date;
import java.util.Set;

import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.runner.Runner;
import seedu.address.model.tag.Tag;

/**
 * Represents a customer in the address book.
 * Guarantees: details are present and not null, field values are validated, immutable.
 */
public class Customer extends Person {

    private final MoneyBorrowed moneyBorrowed;
    private final Date oweStartDate;
    private final Date oweDueDate;
    private final StandardInterest standardInterest; //in percent
    private final LateInterest lateInterest; //in percent
    private final Runner runner;

    /**
     * customer constructor
     */
    public Customer() {
        super();
        this.moneyBorrowed = new MoneyBorrowed();
        this.oweStartDate = new Date();
        this.oweDueDate = new Date();
        this.standardInterest = new StandardInterest();
        this.lateInterest = new LateInterest();
        this.runner = new Runner();
    }

    public Customer(Name name, Phone phone, Email email, Address address, Set<Tag> tags,
                    MoneyBorrowed moneyBorrowed, Date oweStartDate, Date oweDueDate, StandardInterest
                            standardInterest, LateInterest lateInterest, Runner runner) {
        super(name, phone, email, address, tags);
        this.moneyBorrowed = moneyBorrowed;
        this.standardInterest = standardInterest;
        this.lateInterest = lateInterest;
        this.oweStartDate = oweStartDate;
        this.oweDueDate = oweDueDate;
        this.runner = runner;
    }

    public MoneyBorrowed getMoneyBorrowed() {
        return moneyBorrowed;
    }

    public StandardInterest getStandardInterest() {
        return standardInterest;
    }

    public Date getOweStartDate() {
        return oweStartDate;
    }

    public Date getOweDueDate() {
        return oweDueDate;
    }

    public LateInterest getLateInterest() {
        return lateInterest;
    }

    public Runner getRunner() {
        return runner;
    }

    /**
     * @return amount of money owed, after compounded standardInterest, based on num of weeks that has passed since
     * oweStartDate
     */
    public double getMoneyCurrentlyOwed() {
        final int numOfMsPerWeek = 60 * 60 * 24 * 7 * 1000; //10080 seconds per week; 1000 ms per second

        Date currentDate = new Date();
        long elapsedTime = currentDate.getTime() - oweStartDate.getTime();
        long elapsedWeeks = elapsedTime / numOfMsPerWeek;
        return moneyBorrowed.value * Math.pow(1 + standardInterest.value / 100, (double) elapsedWeeks);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getName())
                .append(" Phone: ")
                .append(getPhone())
                .append(" Email: ")
                .append(getEmail())
                .append(" Address: ")
                .append(getAddress())
                .append(" Money Owed: ")
                .append(getMoneyCurrentlyOwed())
                .append(" Standard Interest Rate: ")
                .append(getStandardInterest())
                .append(" Start Date: ")
                .append(getOweStartDate())
                .append(" Due Date: ")
                .append(getOweDueDate())
                .append(" runner: ")
                .append(runner.getName())
                .append(" Tags: ");
        getTags().forEach(builder::append);
        return builder.toString();
    }
}
