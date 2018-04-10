package seedu.address.model.util;

import static java.util.Calendar.MARCH;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.customer.Customer;
import seedu.address.model.person.customer.LateInterest;
import seedu.address.model.person.customer.MoneyBorrowed;
import seedu.address.model.person.customer.StandardInterest;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.runner.Runner;
import seedu.address.model.tag.Tag;

//@@author melvintzw

/**
 * Contains utility methods for populating {@code AddressBook} with sample data.
 */
public class SampleDataUtil {
    public static Person[] getSamplePersons() {
        return new Person[]{
            new Customer(new Name("Alex Yeoh"), new Phone("87438807"), new Email("alexyeoh@example.com"),
                    new Address("Blk 30 Geylang Street 29, #06-40"),
                    getTagSet("HighValue", "Happy"), new MoneyBorrowed(100), defaultDate(), new Date(),
                    new StandardInterest(1), new LateInterest(), new Runner()),
            new Customer(new Name("Jonathan Lee"), new Phone("999"), new Email("motherchicken@example.com"),
                    new Address("Marina Bay Sands"),
                    getTagSet("Rich"), new MoneyBorrowed(100), defaultDate(), new Date(),
                    new StandardInterest(1), new LateInterest(), new Runner()),
            new Customer(new Name("Xiao Ming"), new Phone("88819991"), new Email("nigerian_prince@bankofchina.com"),
                    new Address("Chinatown"),
                    getTagSet("Handsome", "Pretty"), new MoneyBorrowed(100), defaultDate(), new Date(),
                    new StandardInterest(1), new LateInterest(), new Runner()),
            new Runner(new Name("Choi Wi Su"), new Phone("99994321"), new Email("hotkorean1995@hotmail.com"),
                    new Address("I'm Kim"), getTagSet("RightHandMan"), new ArrayList<>()),
            new Runner(new Name("Ah Huat"), new Phone("88888888"), new Email("quick_and_easy_money@hotmail.com"),
                    new Address("Botanic Gardens"), getTagSet("EmployeeOfTheMonth"), new ArrayList<>())
        };
    }

    /**
     * helper method to generate a meaningful date. Currently hard-coded for 1 March 2018.
     *
     * @return
     */
    private static Date defaultDate() {
        int year = 2018;
        int month = MARCH;
        int dayOfMonth = 1;
        int hourOfDay = 0;
        int minute = 0;
        int second = 0;

        GregorianCalendar calendar = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
        return calendar.getTime();
    }

    /**
     * helper method to generate a custom meaningful date.
     *
     * @return
     */
    private static Date createDate(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
        return calendar.getTime();
    }
    //@@author

    public static ReadOnlyAddressBook getSampleAddressBook() {
        try {
            AddressBook sampleAb = new AddressBook();
            for (Person samplePerson : getSamplePersons()) {
                sampleAb.addPerson(samplePerson);
            }
            return sampleAb;
        } catch (DuplicatePersonException e) {
            throw new AssertionError("sample data cannot contain duplicate persons", e);
        }
    }

    /**
     * Returns a tag set containing the list of strings given.
     */
    public static Set<Tag> getTagSet(String... strings) {
        HashSet<Tag> tags = new HashSet<>();
        for (String s : strings) {
            tags.add(new Tag(s));
        }

        return tags;
    }

}
