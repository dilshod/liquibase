package liquibase.change;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.List;

import liquibase.ChangeSet;
import liquibase.FileOpener;
import liquibase.database.Database;
import liquibase.database.statement.SqlStatement;
import liquibase.database.statement.visitor.SqlVisitor;
import liquibase.database.structure.DatabaseObject;
import liquibase.exception.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Interface all changes (refactorings) implement.
 * <p/>
 * <b>How changes are constructed and run when reading changelogs:</b>
 * <ol>
 * <li>As the changelog handler gets to each element inside a changeSet, it passes the tag name to liquibase.change.ChangeFactory
 * which looks through all the registered changes until it finds one with matching specified tag name</li>
 * <li>The ChangeFactory then constructs a new instance of the change</li>
 * <li>For each attribute in the XML node, reflection is used to call a corresponding set* method on the change class</li>
 * <li>The correct generateStatements(*) method is called for the current database</li>
 * </ol>
 * <p/>
 * <b>To implement a new change:</b>
 * <ol>
 * <li>Create a new class that implements Change (normally extend AbstractChange)</li>
 * <li>Implement the abstract generateStatements(*) methods which return the correct SQL calls for each database</li>
 * <li>Implement the createMessage() method to create a descriptive message for logs and dialogs
 * <li>Implement the createNode() method to generate an XML element based on the values in this change</li>
 * <li>Add the new class to the liquibase.change.ChangeFactory</li>
 * </ol>
 * <p><b>Implementing automatic rollback support</b><br><br>
 * The easiest way to allow automatic rollback support is by overriding the createInverses() method.
 * If there are no corresponding inverse changes, you can override the generateRollbackStatements(*) and canRollBack() methods.
 * <p/>
 * <b>Notes for generated SQL:</b><br>
 * Because migration and rollback scripts can be generated for execution at a different time, or against a different database,
 * changes you implement cannot directly reference data in the database.  For example, you cannot implement a change that selects
 * all rows from a database and modifies them based on the primary keys you find because when the SQL is actually run, those rows may not longer
 * exist and/or new rows may have been added.
 * <p/>
 * We chose the name "change" over "refactoring" because changes will sometimes change functionality whereas true refactoring will not.
 *
 * @see ChangeFactory
 * @see Database
 */
public interface Change {

    public static final int SPECIALIZATION_LEVEL_DEFAULT = 1;
    public static final int SPECIALIZATION_LEVEL_DATABASE_SPECIFIC = 5;

    /**
     * @return A descripton of the change
     */
    public String getChangeDescription();

    /**
     * @return The "name" of the change.  The name is used for looking up a change based on an XML tag and other times
     *         where you want to dynamically generate a Change implementation.
     */
    public String getChangeName();

    /**
     * @return The "speciliazation" that this change is designed for.  Higher specialiazations will take precidence.
     */
    public int getSpecializationLevel();

    boolean supports(Database database);
    
    /**
     * This method will be called after the no arg constructor and all of the
     * properties have been set to allow the task to do any heavy tasks or
     * more importantly generate any exceptions to report to the user about
     * the settings provided.
     */
    public void setUp() throws SetupException;

    public void validate(Database database) throws InvalidChangeDefinitionException;

    public ChangeSet getChangeSet();

    public void setChangeSet(ChangeSet changeSet);

    /**
     * Sets the fileOpener that should be used for any file loading and resource
     * finding for files that are provided by the user.
     */
    public void setFileOpener(FileOpener fileOpener);

    public Set<DatabaseObject> getAffectedDatabaseObjects(Database database);
    
    /**
     * Calculates the checksum (currently MD5 hash) for the current configuration of this change.
     */
    public String generateCheckSum();

     /**
     * @return Confirmation message to be displayed after the change is executed
     */
    public String getConfirmationMessage();

    /**
     * Generates the SQL statements required to run the change
     *
     * @param database databasethe target {@link Database} associated to this change's statements
     * @return an array of {@link String}s with the statements
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     */
    public SqlStatement[] generateStatements(Database database) throws UnsupportedChangeException;

     /**
     * Can this change be rolled back
     *
     * @return <i>true</i> if rollback is supported, <i>false</i> otherwise
     */
    public boolean supportsRollback();

    /**
     * Generates the SQL statements required to roll back the change
     *
     * @param database database databasethe target {@link Database} associated to this change's rollback statements
     * @return an array of {@link String}s with the rollback statements
     * @throws UnsupportedChangeException  if this change is not supported by the {@link Database} passed as argument
     * @throws RollbackImpossibleException if rollback is not supported for this change
     */
    public SqlStatement[] generateRollbackStatements(Database database) throws UnsupportedChangeException, RollbackImpossibleException;

    /**
     * Creates an XML element (of type {@link Node}) of the change object, and adds it
     * to the {@link Document} object passed as argument
     *
     * @param currentChangeLogDOM the current {@link Document} where this element is being added
     * @return the {@link Node} object created
     */
    public Node createNode(Document currentChangeLogDOM);

}
