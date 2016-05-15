package cz.muni.fi.pv168.librarymanager.backend;

import java.time.Clock;
import java.time.LocalDate;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 *
 * @author Josef Pavelec, Faculty of Informatics, Masaryk University
 */
public class Main {
    
    final static Logger log = LoggerFactory.getLogger(Main.class);
    private static final Clock clock = Clock.systemDefaultZone();
    
    public static DataSource createMemoryDatabase() {
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(EmbeddedDriver.class.getName());
        bds.setUrl("jdbc:derby:memory:library;create=true");
        new ResourceDatabasePopulator(
                new ClassPathResource("createTables.sql"),
                new ClassPathResource("testData.sql"))
                .execute(bds);
        return bds;
    }

    public static void main(String[] args)  {

        log.info("zaciname");
        DataSource dataSource = createMemoryDatabase();
        
        RentManagerImpl rentManager = new RentManagerImpl(clock);

    }

}




