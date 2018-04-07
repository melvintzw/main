package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.commands.EditCommand.MESSAGE_DUPLICATE_PERSON;
import static seedu.address.logic.parser.CliSyntax.PREFIX_CUSTOMERS;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.address.commons.core.Messages;
import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.CollectionUtil;
import seedu.address.logic.commands.exceptions.CommandException;
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
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.model.person.runner.Runner;
import seedu.address.model.tag.Tag;

//@@author melvintzw

/**
 * Adds customers to a runner's customer list , list must contain unique elements
 */
public class AssignCommand extends UndoableCommand implements PopulatableCommand {

    public static final String COMMAND_WORD = "assign";
    public static final String COMMAND_ALIAS = "as";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": assigns customers to a runner "
            + "by the index number used in the last person listing.\n"
            + "Parameters: RUNNER-INDEX (positive integer) "
            + PREFIX_CUSTOMERS + " CUSTOMER-INDEX (positive integer) "
            + "[ CUSTOMER-2-INDEX...]\n"
            + "Example: " + COMMAND_WORD + " 5 " + PREFIX_CUSTOMERS + " 2 ";

    public static final String MESSAGE_ASSIGN_PERSON_SUCCESS = "Successfully assigned!\nUpdated Runner Info:\n%1$s";
    // message

    private final Index runnerIndex;
    private final Index[] customerIndex;

    private List<Person> oldCustomers = new ArrayList<>(); //customers already in runner's list of customers
    private List<Person> newCustomers = new ArrayList<>(); //customers to be added to runner's list of customers
    private List<Person> updatedCustomers = new ArrayList<>(); //new customers that have been been updated with runner
    private List<EditPersonDescriptor> listOfEditedCustDesc = new ArrayList<>();

    private Person personToEdit;
    private Person editedPerson;
    private EditPersonDescriptor editRunnerDescriptor = new EditPersonDescriptor();

    /**
     * @param runnerIndex of the Runner in the filtered person list to edit
     * @param customerIndex ... of the customers to add to Runner's customer list
     */
    public AssignCommand(Index runnerIndex, Index... customerIndex) {
        requireNonNull(runnerIndex);
        requireNonNull(customerIndex);

        this.runnerIndex = runnerIndex;
        this.customerIndex = customerIndex;
    }

    /**
     * For call in PopulatePrefixRequestEvent class, to assign string values.
     */
    public AssignCommand() {
        runnerIndex = null;
        customerIndex = null;
    }

    @Override
    public CommandResult executeUndoableCommand() throws CommandException {
        try {
            deletePrevRunnerCustomer(); //this must be called BEFORE updating assigned customers in the loop below
            model.updatePerson(personToEdit, editedPerson);
            int i = 0;
            for (Person c : newCustomers) {
                model.updatePerson(c, updatedCustomers.get(i));
                i++;
            }

        } catch (DuplicatePersonException dpe) {
            throw new CommandException(MESSAGE_DUPLICATE_PERSON);
        } catch (PersonNotFoundException pnfe) {
            throw new AssertionError("The target person cannot be missing");
        }
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        return new CommandResult(String.format(MESSAGE_ASSIGN_PERSON_SUCCESS, editedPerson));
    }

    @Override
    protected void preprocessUndoableCommand() throws CommandException {
        List<Person> lastShownList = model.getFilteredPersonList();

        if (runnerIndex.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        personToEdit = lastShownList.get(runnerIndex.getZeroBased());

        if (!(personToEdit instanceof Runner)) {
            throw new CommandException(String.format("Person at index %d is not a Runner", runnerIndex.getOneBased()));
        }
        //NOTE: it is important to call these methods in this order so that the appropriate resources are generated
        generateNewCustomerList();
        generateCustDescWithAssignedRunner();
        generateUpdatedCustomerList();
        makeEditRunnerDescriptorFromUpdatedCustList(); //modifies editRunnerDescriptor
        editedPerson = createEditedPerson(personToEdit, editRunnerDescriptor);
    }

    /**
     * Since each Customer should only have 1 Runner, if the customer had a runner previously assigned, then that
     * previous runner should have its association with this customer removed. The customer will now only be associated
     * with the newly assigned runner.
     */
    private void deletePrevRunnerCustomer() throws CommandException, PersonNotFoundException,
            DuplicatePersonException {
        List<Person> pl = model.getAddressBook().getPersonList();
        //List<Person> allCustomers = new ArrayList<>();
        //allCustomers.addAll(oldCustomers);
        //allCustomers.addAll(newCustomers);
        for (Person c : newCustomers) {
            Person r = ((Customer) c).getRunner(); //not getting a runner from pl but an incomplete copy
            int indexOfActualPerson = pl.indexOf(r);

            if (indexOfActualPerson >= 0) {
                //the conditional check is necessary so that I'm only modifying valid existing runners

                Person actualRunner =  pl.get(indexOfActualPerson); //getting the actual complete runner from pl

                //generate editPersonDescriptor with c removed from runner's customer list
                EditPersonDescriptor runnerDescWCustRemoved = new EditPersonDescriptor();

                runnerDescWCustRemoved.setName(actualRunner.getName());
                runnerDescWCustRemoved.setPhone(actualRunner.getPhone());
                runnerDescWCustRemoved.setEmail(actualRunner.getEmail());
                runnerDescWCustRemoved.setAddress(actualRunner.getAddress());
                runnerDescWCustRemoved.setTags(actualRunner.getTags());

                List<Person> newList = ((Runner) actualRunner).getCustomers();
                newList.remove(c);
                runnerDescWCustRemoved.setCustomers(newList);

                Person editedPrevRunner = createEditedPerson((Runner) actualRunner, runnerDescWCustRemoved);
                model.updatePerson(actualRunner, editedPrevRunner);
            }
        }
    }

    /**
     * Edit each new customer with the runner to be assigned.
     *
     * Requires an accompanying list of customer descriptors describing these new customers and reflecting the assigned
     * runner.
     * @throws CommandException
     */
    private void generateUpdatedCustomerList() throws CommandException {
        int i = 0;
        for (Person c : newCustomers) {
            updatedCustomers.add(createEditedPerson(c, listOfEditedCustDesc.get(i)));
            i++;
        }
    }

    /**
     * Creates and returns an {@code EditPersonDescriptor} with new customers from customerIndex...
     * the created EditPersonDescriptor is to be used to create editedPerson.
     */
    private void makeEditRunnerDescriptorFromUpdatedCustList() throws CommandException {
        List<Person> lastShownList = model.getFilteredPersonList();
        Person runnerToBeEdited = lastShownList.get(runnerIndex.getZeroBased());
        assert (runnerToBeEdited instanceof Runner);

        editRunnerDescriptor.setName(runnerToBeEdited.getName());
        editRunnerDescriptor.setPhone(runnerToBeEdited.getPhone());
        editRunnerDescriptor.setEmail(runnerToBeEdited.getEmail());
        editRunnerDescriptor.setAddress(runnerToBeEdited.getAddress());
        editRunnerDescriptor.setTags(runnerToBeEdited.getTags());

        List<Person> allCustomers = new ArrayList<>();
        allCustomers.addAll(oldCustomers);
        allCustomers.addAll(updatedCustomers);
        editRunnerDescriptor.setCustomers(allCustomers);
    }

    /**
     * generates a list of new and unique customers to be assigned to the runner.
     * @throws CommandException
     */
    private void generateNewCustomerList() throws CommandException {
        List<Person> lastShownList = model.getFilteredPersonList();
        Person runnerToBeEdited = lastShownList.get(runnerIndex.getZeroBased());
        oldCustomers.addAll(((Runner) runnerToBeEdited).getCustomers());

        for (Index index: customerIndex) {
            Person p = lastShownList.get(index.getZeroBased());
            if (!(p instanceof Customer)) {
                throw new CommandException("invalid customer index");
            }
            if (oldCustomers.indexOf(p) >= 0) {
                throw new CommandException(String.format("one or more customers already assigned to runner",
                        index.getOneBased()));
            }
            if (newCustomers.indexOf(p) >= 0) {
                throw new CommandException("cannot assign same customer twice");
            }
            newCustomers.add((Customer) p);
        }
    }

    /**
     * Generates a list of EditPersonDescriptors for the purpose of updating each customer with the assigned runner
     * This helper method is meant to be called in executeUndoableCommand().
     * references to each other.
     */
    private void generateCustDescWithAssignedRunner() {
        List<Person> lastShownList = model.getFilteredPersonList();
        Person runnerToBeEdited = lastShownList.get(runnerIndex.getZeroBased());
        assert (runnerToBeEdited instanceof Runner);
        for (Person c : newCustomers) {
            EditPersonDescriptor custDesc = new EditPersonDescriptor();

            custDesc.setRunner((Runner) runnerToBeEdited);

            custDesc.setName(c.getName());
            custDesc.setPhone(c.getPhone());
            custDesc.setEmail(c.getEmail());
            custDesc.setAddress(c.getAddress());
            custDesc.setTags(c.getTags());

            custDesc.setMoneyBorrowed(((Customer) c).getMoneyBorrowed());
            custDesc.setOweStartDate(((Customer) c).getOweStartDate());
            custDesc.setOweDueDate(((Customer) c).getOweDueDate());
            custDesc.setStandardInterest(((Customer) c).getStandardInterest());
            custDesc.setLateInterest(((Customer) c).getLateInterest());

            listOfEditedCustDesc.add(custDesc);
        }
    }

    /**
     * Creates and returns a {@code Person} with the details of {@code personToEdit}
     * edited with {@code editRunnerDescriptor}.
     * This method is borrowed from EditCommand
     */
    private static Person createEditedPerson(Person personToEdit, EditPersonDescriptor editPersonDescriptor) throws
            CommandException {

        assert personToEdit != null;

        Name updatedName = editPersonDescriptor.getName().orElse(personToEdit.getName());
        Phone updatedPhone = editPersonDescriptor.getPhone().orElse(personToEdit.getPhone());
        Email updatedEmail = editPersonDescriptor.getEmail().orElse(personToEdit.getEmail());
        Address updatedAddress = editPersonDescriptor.getAddress().orElse(personToEdit.getAddress());
        Set<Tag> updatedTags = editPersonDescriptor.getTags().orElse(personToEdit.getTags());

        if (personToEdit instanceof Customer) {

            MoneyBorrowed moneyBorrowed = editPersonDescriptor.getMoneyBorrowed().orElse(((Customer) personToEdit)
                    .getMoneyBorrowed());
            Date oweStartDate = editPersonDescriptor.getOweStartDate().orElse(((Customer) personToEdit)
                    .getOweStartDate());
            Date oweDueDate = editPersonDescriptor.getOweDueDate().orElse(((Customer) personToEdit)
                    .getOweDueDate());
            StandardInterest standardInterest = editPersonDescriptor.getStandardInterest()
                    .orElse(((Customer) personToEdit).getStandardInterest());
            LateInterest lateInterest = editPersonDescriptor.getLateInterest().orElse(((Customer) personToEdit)
                    .getLateInterest());
            Person runner = editPersonDescriptor.getRunner().orElse(((Customer) personToEdit)
                    .getRunner());

            return new Customer(updatedName, updatedPhone, updatedEmail, updatedAddress, updatedTags, moneyBorrowed,
                    oweStartDate, oweDueDate, standardInterest, lateInterest, runner);

        } else if (personToEdit instanceof Runner) {

            List<Person> customers = editPersonDescriptor.getCustomers().orElse(((Runner) personToEdit)
                    .getCustomers());

            return new Runner(updatedName, updatedPhone, updatedEmail, updatedAddress, updatedTags, customers);

        } else {
            throw new CommandException("Error: Invalid Person");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssignCommand)) {
            return false;
        }
        AssignCommand that = (AssignCommand) o;
        return Objects.equals(runnerIndex, that.runnerIndex)
                && Arrays.equals(customerIndex, that.customerIndex)
                && Objects.equals(oldCustomers, that.oldCustomers)
                && Objects.equals(newCustomers, that.newCustomers)
                && Objects.equals(updatedCustomers, that.updatedCustomers)
                && Objects.equals(listOfEditedCustDesc, that.listOfEditedCustDesc)
                && Objects.equals(personToEdit, that.personToEdit)
                && Objects.equals(editedPerson, that.editedPerson)
                && Objects.equals(editRunnerDescriptor, that.editRunnerDescriptor);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(runnerIndex, oldCustomers, newCustomers, updatedCustomers, listOfEditedCustDesc,
                personToEdit, editedPerson, editRunnerDescriptor);
        result = 31 * result + Arrays.hashCode(customerIndex);
        return result;
    }

    @Override
    public String getCommandWord() {
        return COMMAND_WORD;
    }

    @Override
    public String getTemplate() {
        return COMMAND_WORD + "  " + PREFIX_CUSTOMERS + " ";
    }

    @Override
    public int getCaretIndex() {
        return (COMMAND_WORD + " ").length();
    }

    @Override
    public String getUsageMessage() {
        return MESSAGE_USAGE;
    }


    /**
     * Stores the details to edit the person with. Each non-empty field value will replace the
     * corresponding field value of the person.
     */
    public static class EditPersonDescriptor {
        private Name name;
        private Phone phone;
        private Email email;
        private Address address;
        private Set<Tag> tags;

        //Customer fields
        private MoneyBorrowed moneyBorrowed;
        private Date oweStartDate;
        private Date oweDueDate;
        private StandardInterest standardInterest;
        private LateInterest lateInterest;
        private Person runner;

        //Runner fields
        private List<Person> customers;

        public EditPersonDescriptor() {}

        /**
         * Copy constructor.
         * A defensive copy of {@code tags} is used internally.
         */
        public EditPersonDescriptor(EditPersonDescriptor toCopy) {
            setName(toCopy.name);
            setPhone(toCopy.phone);
            setEmail(toCopy.email);
            setAddress(toCopy.address);
            setTags(toCopy.tags);

            setMoneyBorrowed(toCopy.moneyBorrowed);
            setOweStartDate(toCopy.oweStartDate);
            setOweDueDate(toCopy.oweDueDate);
            setStandardInterest(toCopy.standardInterest);
            setLateInterest(toCopy.lateInterest);
            setRunner(toCopy.runner);

            setCustomers(toCopy.customers);
        }

        /**
         * Returns true if at least one field is edited.
         */
        public boolean isAnyFieldEdited() {
            return CollectionUtil.isAnyNonNull(this.name, this.phone, this.email, this.address, this.tags,
                    this.moneyBorrowed, this.oweStartDate, this.oweDueDate, this.standardInterest, this.lateInterest,
                    this.runner);
        }

        public void setName(Name name) {
            this.name = name;
        }
        public Optional<Name> getName() {
            return Optional.ofNullable(name);
        }

        public void setPhone(Phone phone) {
            this.phone = phone;
        }
        public Optional<Phone> getPhone() {
            return Optional.ofNullable(phone);
        }

        public void setEmail(Email email) {
            this.email = email;
        }
        public Optional<Email> getEmail() {
            return Optional.ofNullable(email);
        }

        public void setAddress(Address address) {
            this.address = address;
        }
        public Optional<Address> getAddress() {
            return Optional.ofNullable(address);
        }

        public void setMoneyBorrowed(MoneyBorrowed moneyBorrowed) {
            this.moneyBorrowed = moneyBorrowed;
        }
        public Optional<MoneyBorrowed> getMoneyBorrowed() {
            return Optional.ofNullable(moneyBorrowed);
        }

        public void setOweStartDate(Date oweStartDate) {
            this.oweStartDate = oweStartDate;
        }
        public Optional<Date> getOweStartDate() {
            return Optional.ofNullable(oweStartDate);
        }

        public void setOweDueDate(Date oweDueDate) {
            this.oweDueDate = oweDueDate;
        }
        public Optional<Date> getOweDueDate() {
            return Optional.ofNullable(oweDueDate);
        }

        public void setStandardInterest(StandardInterest standardInterest) {
            this.standardInterest = standardInterest;
        }
        public Optional<StandardInterest> getStandardInterest() {
            return Optional.ofNullable(standardInterest);
        }

        public void setLateInterest(LateInterest lateInterest) {
            this.lateInterest = lateInterest;
        }
        public Optional<LateInterest> getLateInterest() {
            return Optional.ofNullable(lateInterest);
        }

        public void setRunner(Person runner) {
            this.runner = runner;
        }
        public Optional<Person> getRunner() {
            return Optional.ofNullable(runner);
        }

        public void setCustomers(List<Person> customers) {
            this.customers = customers;
        }
        public Optional<List<Person>> getCustomers() {
            return Optional.ofNullable(customers);
        }

        /**
         * Sets {@code tags} to this object's {@code tags}.
         * A defensive copy of {@code tags} is used internally.
         */
        public void setTags(Set<Tag> tags) {
            this.tags = (tags != null) ? new HashSet<>(tags) : null;
        }

        /**
         * Returns an unmodifiable tag set, which throws {@code UnsupportedOperationException}
         * if modification is attempted.
         * Returns {@code Optional#empty()} if {@code tags} is null.
         */
        public Optional<Set<Tag>> getTags() {
            return (tags != null) ? Optional.of(Collections.unmodifiableSet(tags)) : Optional.empty();
        }

        @Override
        public boolean equals(Object other) {
            // short circuit if same object
            if (other == this) {
                return true;
            }

            // instanceof handles nulls
            if (!(other instanceof EditPersonDescriptor)) {
                return false;
            }

            // state check
            EditPersonDescriptor e = (EditPersonDescriptor) other;

            return getName().equals(e.getName())
                    && getPhone().equals(e.getPhone())
                    && getEmail().equals(e.getEmail())
                    && getAddress().equals(e.getAddress())
                    && getTags().equals(e.getTags())
                    && getMoneyBorrowed().equals(e.getMoneyBorrowed())
                    && getOweDueDate().equals(e.getOweDueDate())
                    && getOweStartDate().equals(e.getOweStartDate())
                    && getStandardInterest().equals(e.getStandardInterest())
                    && getLateInterest().equals(e.getLateInterest())
                    && getRunner().equals(e.getRunner())
                    && getCustomers().equals(e.getCustomers());

        }
    }
}
