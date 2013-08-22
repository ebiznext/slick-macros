slick-macros
============

- The @Model  macro provide an easy way to create Slick table objects. The code below will produce all the slick 
  boilerplate code including the table object, the foreign keys, the association table for many to many relationships and 
  the Enumeration Type Mapper

    ```scala
@Model object XDb {
  object UserRights extends Enumeration {
    type UserRights = Value
    val ADMIN = Value(1)
    val GUEST = Value(2)
  }
  import UserRights._
  case class Company(name: String, website: String)
  case class Member(login: String, rights: UserRights, company: Company)
  case class Project(name: String, company: Company, members: List[Member])
}

    ```
    The code above will be processed by applying the following rules :
    - A type mapper is generated for each Enumeration, so that they can be used as table columns (only Int values are handled for now)
    - an attribute that references another case class is converted to a foreign key
    - an attribute that references a collection of objects of another case class triggers the creation of a assoc table (many2many relationship)
    - a Slick table object (including the forInsert method) is generated for each case class
    - timestamps columns (dateCreated & lastUpdated) are also created but this feature is commented in the source code until the DynamicAccessor trait below is ready
    
  The generated AST is equivalent to the code below (that would have to be written by hand otherwise) :

    ```scala
    object XDb {
  object UserRights extends Enumeration {
    type UserRights = Value;
    val ADMIN = Value(1);
    val GUEST = Value(2)
  }
  
  implicit val UserRightsTypeMapper = MappedTypeMapper.base[UserRights.Value, Int](
    {
      it => it.id
    },
    {
      id => UserRights(id)
    })
    
  import UserRights._

  case class Company(id: Option[Int], name: String, website: String)

  object Companies extends Table[Company]("company") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def website = column[String]("website")
    def * = id.? ~ name ~ website <> (Company, (Company.unapply _))

    def forInsert = name ~ website <> (((t) => Company(None, t._1, t._2)), ((x: Company) => Some((x.name, x.website))))
  }

  case class Member(id: Option[Int], login: String, rights: UserRights, companyId: Int)

  object Members extends Table[Member]("member") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def login = column[String]("login")
    def rights = column[UserRights]("rights")
    def companyId = column[Int]("companyId")
    def * = id.? ~ login ~ rights ~ companyId <> (Member, (Member.unapply _))

    def forInsert = login ~ rights ~ companyId <> (((t) => Member(None, t._1, t._2, t._3)), ((x: Member) => Some((x.login, x.rights, x.companyId))))

    def company = foreignKey("member2company", companyId, Companies)(_.id)
  }

  case class Project(id: Option[Int], name: String, companyId: Int)

  object Projects extends Table[Project]("project") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def companyId = column[Int]("companyId")
    def * = id.? ~ name ~ companyId <> (Project, (Project.unapply _))
    
    def forInsert = name ~ companyId <> (((t) => Project(None, t._1, t._2)), ((x: Project) => Some((x.name, x.companyId))))

    def company = foreignKey("project2company", companyId, Companies)(_.id)
  }

  case class Project2Member(val projectId: Int, val memberId: Int)
  object Project2Members extends Table[Project2Member]("project2member") {
    def projectId = column[Int]("projectId")
    def memberId = column[Int]("memberId")
    def * = projectId ~ memberId <> (Project2Member, (Project2Member.unapply _))
    
    def project = foreignKey("project2member2project", projectId, Projects)(_.id)
    def member = foreignKey("project2member2member", memberId, Members)(_.id)
  }
}
    ```
The SQL Schema produced by Slick for the Pg database is visible below :

  ```sql
create table "company" ("id" SERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL,"website" VARCHAR(254) NOT NULL)
create table "member" ("id" SERIAL NOT NULL PRIMARY KEY,"login" VARCHAR(254) NOT NULL,"rights" INTEGER NOT NULL,"companyId" INTEGER NOT NULL)
create table "project2member" ("projectId" INTEGER NOT NULL,"memberId" INTEGER NOT NULL)
alter table "member" add constraint "member2company" foreign key("companyId") references "company"("id") on update NO ACTION on delete NO ACTION
alter table "project2member" add constraint "project2member2member" foreign key("memberId") references "member"("id") on update NO ACTION on delete NO ACTION
alter table "project2member" add constraint "project2member2project" foreign key("projectId") references "project"("id") on update NO ACTION on delete NO ACTION

  ```

- @SessionOnly (@Transactional) when put in front of a service will inject the database (transactional) session around the service code.
  The code below

    ```scala
    @Transactional def myService(...) = body
    @SessionOnly def myService2(...) = body2
    ```
    
    will generate the AST for the code below
    ```scala
    def myService(...) = Database.for[URL|Driver|Name|DataSource](...) withTransaction { body }
    def myService2(...) = Database.for[URL|Driver|Name|DataSource](...) withSession { body2 }
    ```
    
    The database connection infos has to be provided through an implicit value of the type DbConnectionInfos
    ```scala
    case class DbConnectionInfos(
      jndiName: String = null,
      dataSource: DataSource = null,
      url: String = null,
      user: String = null,
      password: String = null,
      driverClassName: String = null,
      driver: Driver = null,
      properties: Properties = null)
    ```
    
    For a JNDI datasource simply provide the implicit as below :
    ```scala
      implicit val myJndiDBConnectionInfo = DbConnectionInfos(jndiName = "vars/jndi/jdbc/tetradb")
    ```
    
    For a URL datasource simply provide the implicit as below :
    ```scala
  implicit val myUrlDBConnectionInfo = DbConnectionInfos(url="jdbc:postgresql:tetra", driver = "org.postgresql.Driver",
                                                        user = "tetra", password = "e-z12B24")
    ```
    
- The DynamicAccessor (Work in progress)  Trait will inject timestamps into insert and update statements and allow updates
  to be written in a syntax similar to the one below :
    ```scala
    q.update(name="him", age=20)
    ```
  instead of
    ```scala
    q.map { row => 
      row.name ~ row.age 
    }.update(("him", 20)) 
```


