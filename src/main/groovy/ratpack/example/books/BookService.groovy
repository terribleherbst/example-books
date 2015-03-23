package ratpack.example.books
import groovy.json.JsonSlurper
import groovy.sql.GroovyRowResult
import groovy.util.logging.Slf4j
import rx.Observable

import javax.inject.Inject

@Slf4j
class BookService {

    private final BookDbCommands bookDbCommands
    private final IsbnDbCommands isbnDbCommands

    @Inject
    BookService(BookDbCommands bookDbCommands, IsbnDbCommands isbnDbCommands) {
        this.bookDbCommands = bookDbCommands
        this.isbnDbCommands = isbnDbCommands
    }

    void createTable() {
        log.info("Creating database tables")
        bookDbCommands.createTables()
    }

    void init(final String isbn, final long quantity, final BigDecimal price) {
        bookDbCommands.init(isbn,quantity,price)
    }

    Observable<Book> all() {
        bookDbCommands.getAll().flatMap {
            isbnDbCommands.getBookRequest(it.isbn).map { String jsonResp ->
                def result = new JsonSlurper().parseText(jsonResp)
                return new Book(
                        it.isbn,
                        it.quantity,
                        it.price,
                        result.data[0].title,
                        result.data[0].author_data[0].name,
                        result.data[0].publisher_name
                )
            }
        }
    }

    Observable<String> insert(String isbn, long quantity, BigDecimal price) {
        bookDbCommands.insert(isbn, quantity, price).map {
            isbn
        }
    }

    Observable<Book> find(String isbn) {
        Observable.zip(
                bookDbCommands.find(isbn),
                isbnDbCommands.getBookRequest(isbn)
        ) { GroovyRowResult dbRow, String jsonResp ->
            def result = new JsonSlurper().parseText(jsonResp)
            return new Book(
                    isbn,
                    dbRow.quantity,
                    dbRow.price,
                    result.data[0].title,
                    result.data[0].author_data[0].name,
                    result.data[0].publisher_name
            )
        }
    }

    Observable<Void> update(String isbn, long quantity, BigDecimal price) {
        bookDbCommands.update(isbn, quantity, price)
    }

    Observable<Void> delete(String isbn) {
        bookDbCommands.delete(isbn)
    }
}
