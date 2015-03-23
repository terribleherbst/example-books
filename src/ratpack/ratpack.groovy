import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.zaxxer.hikari.HikariConfig
import org.pac4j.http.client.FormClient
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.codahale.healthcheck.CodaHaleHealthCheckModule
import ratpack.codahale.healthcheck.HealthCheckHandler
import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.codahale.metrics.MetricsWebsocketBroadcastHandler
import ratpack.config.ConfigData
import ratpack.error.ServerErrorHandler
import ratpack.example.books.*
import ratpack.form.Form
import ratpack.groovy.sql.SqlModule
import ratpack.groovy.template.MarkupTemplateModule
import ratpack.handling.RequestId
import ratpack.hikari.HikariModule
import ratpack.hystrix.HystrixMetricsEventStreamHandler
import ratpack.hystrix.HystrixModule
import ratpack.jackson.JacksonModule
import ratpack.pac4j.Pac4jModule
import ratpack.rx.RxRatpack
import ratpack.server.ReloadInformant
import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.session.SessionModule
import ratpack.session.store.MapSessionsModule

import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack

final Logger log = LoggerFactory.getLogger(ratpack.class);

ratpack {
    bindings {
        ConfigData configData = ConfigData.of(new JSR310Module())
                .props("$serverConfig.baseDir.file/application.properties")
                .env()
                .sysProps()
                .build()
        bindInstance(ReloadInformant, configData) // Add to the registry to enable development time config reloading
        bindInstance(IsbndbConfig, configData.get("/isbndb", IsbndbConfig))

        addConfig(CodaHaleMetricsModule, configData.get("/metrics", CodaHaleMetricsModule.Config))
        bind DatabaseHealthCheck
        add CodaHaleHealthCheckModule

        add(HikariModule) { HikariConfig c ->
            c.addDataSourceProperty("URL", "jdbc:h2:mem:dev;INIT=CREATE SCHEMA IF NOT EXISTS DEV")
            c.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource")
        }
        add SqlModule
        add JacksonModule
        add BookModule
        add SessionModule
        add new MapSessionsModule(10, 5)
        add new Pac4jModule<>(new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator()), new AuthPathAuthorizer())
        add MarkupTemplateModule
        add new HystrixModule().sse()
        bind MarkupTemplateRenderableDecorator

        bindInstance Service, new Service() {
            @Override
            void onStart(StartEvent event) throws Exception {
                log.info "Initializing RX"
                RxRatpack.initialize()

                BookService bookService = event.registry.get(BookService)
                bookService.createTable()
                bookService.init("1932394842",100,new BigDecimal("29.99"))
                bookService.init("0471329304",27,new BigDecimal("29.99"))
                bookService.init("1449316387",1,new BigDecimal("29.99"))
                bookService.init("0974514004",2,new BigDecimal("29.99"))

            }
        }

        bind ServerErrorHandler, ErrorHandler
    }

    handlers { BookService bookService ->
        handler(RequestId.bindAndLog()) // log all requests


        /*
        get {
            bookService.all().toList().subscribe { List<Book> books ->
                def isbndbApikey = context.get(IsbndbConfig).apikey

                render groovyMarkupTemplate("listing.gtpl",
                        isbndbApikey: isbndbApikey,
                        title: "Books",
                        books: books,
                        msg: request.queryParams.msg ?: "")
            }
        }
        */

        handler("create") {
            byMethod {
                get {
                    render groovyMarkupTemplate("create.gtpl",
                            title: "Create Book",
                            isbn: '',
                            quantity: '',
                            price: '',
                            method: 'post',
                            action: '',
                            buttonText: 'Create'
                    )
                }
                post {
                    Form form = parse(Form)
                    bookService.insert(
                            form.isbn,
                            form.get("quantity").asType(Long),
                            form.get("price").asType(BigDecimal)
                    ).single().subscribe() { String isbn ->
                        redirect "/?msg=Book+$isbn+created"
                    }
                }
            }
        }

        handler("update/:isbn") {
            def isbn = pathTokens["isbn"]

            bookService.find(isbn).single().subscribe { Book book ->
                if (book == null) {
                    clientError(404)
                } else {
                    byMethod {
                        get {
                            render groovyMarkupTemplate("update.gtpl",
                                    title: "Update Book",
                                    method: 'post',
                                    action: '',
                                    buttonText: 'Update',
                                    isbn: book.isbn,
                                    bookTitle: book.title,
                                    author: book.author,
                                    publisher: book.publisher,
                                    quantity: book.quantity,
                                    price: book.price)
                        }
                        post {
                            Form form = parse(Form)
                            bookService.update(
                                    isbn,
                                    form.get("quantity").asType(Long),
                                    form.get("price").asType(BigDecimal)
                            ) subscribe {
                                redirect "/?msg=Book+$isbn+updated"
                            }
                        }
                    }
                }
            }
        }

        post("delete/:isbn") {
            def isbn = pathTokens["isbn"]
            bookService.delete(isbn).subscribe {
                redirect "/?msg=Book+$isbn+deleted"
            }
        }

        prefix("api/book") {
            handler chain(registry.get(BookRestEndpoint))
        }

        prefix("admin") {
            get("health-check/:name?", new HealthCheckHandler())
            get("metrics-report", new MetricsWebsocketBroadcastHandler())

            get("metrics") {
                render groovyMarkupTemplate("metrics.gtpl", title: "Metrics")
            }
        }
        get("hystrix.stream", new HystrixMetricsEventStreamHandler())

        handler("login") {
            render groovyMarkupTemplate("login.gtpl",
                    title: "Login",
                    action: '/pac4j-callback',
                    method: 'get',
                    buttonText: 'Login',
                    error: request.queryParams.error ?: "")
        }

        assets "public"
    }


}
