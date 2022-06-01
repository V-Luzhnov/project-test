package hw6;

import com.github.javafaker.Faker;
import hw6.api.ProductService;
import hw6.dto.Product;
import hw6.utils.RetrofitUtils;
import lombok.SneakyThrows;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.*;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Backend Java. Homework 6
 *
 * @author Vitalii Luzhnov
 * @version 01.06.2022
 */
public class CreateProductTest {
    static SqlSession session;

    static ProductService productService;
    Product product = null;
    Faker faker = new Faker();
    String category;
    int id;

    @BeforeAll
    static void beforeAll() throws IOException {

        productService = RetrofitUtils.getRetrofit().create(ProductService.class);

        session = null;
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        session = sqlSessionFactory.openSession();
    }

    void setUp() {
        product = new Product()
                .withTitle(faker.food().ingredient())
                .withPrice((int) (Math.random() * 10000))
                .withCategoryTitle(category);
    }

    @Test
    @Tag("Positive")
    @DisplayName("Product creation (Positive)")
    void createProductPositiveTest() throws IOException {
        category = "Food";
        setUp();
        Response<Product> response = productService.createProduct(product).execute();

        assertThat(response.code(), equalTo(201));
        assert response.body() != null;
        assertThat(response.body().getCategoryTitle(), equalTo(category));
        assertThat(response.isSuccessful(), CoreMatchers.is(true));

        id =  response.body().getId();

        db.dao.ProductsMapper productsMapper = session.getMapper(db.dao.ProductsMapper.class);
        db.dao.CategoriesMapper categoriesMapper = session.getMapper(db.dao.CategoriesMapper.class);

        //ищем созданный продукт по ID
        db.model.Products selected = productsMapper.selectByPrimaryKey((long) id);

        //ищем изменяемую категорию по наименованию
        db.model.CategoriesExample example = new db.model.CategoriesExample();
        example.createCriteria().andTitleLike(response.body().getCategoryTitle());
        List<db.model.Categories> list = categoriesMapper.selectByExample(example);
        db.model.Categories categories = list.get(0);
        Long category_id = categories.getId();

        //проверяем реквизиты созданного продукта
        assertThat(selected.getTitle(), equalTo(response.body().getTitle()));
        assertThat(selected.getPrice(), equalTo(response.body().getPrice()));
        assertThat(selected.getCategory_id(), equalTo(category_id));

        //удаляем созданный продукт
        tearDown();

        category = "Electronic";
        setUp();
        response = productService.createProduct(product).execute();

        assertThat(response.code(), equalTo(201));
        assert response.body() != null;
        assertThat(response.body().getCategoryTitle(), equalTo(category));
        assertThat(response.isSuccessful(), CoreMatchers.is(true));

        id = response.body().getId();

        //ищем созданный продукт по ID
        selected = productsMapper.selectByPrimaryKey((long) id);

        //ищем изменяемую категорию по наименованию
        example = new db.model.CategoriesExample();
        example.createCriteria().andTitleLike(response.body().getCategoryTitle());
        list = categoriesMapper.selectByExample(example);
        categories = list.get(0);
        category_id = categories.getId();

        //проверяем реквизиты созданного продукта
        assertThat(selected.getTitle(), equalTo(response.body().getTitle()));
        assertThat(selected.getPrice(), equalTo(response.body().getPrice()));
        assertThat(selected.getCategory_id(), equalTo(category_id));

        //удаляем созданный продукт
        tearDown();
    }

    @SneakyThrows
    void tearDown() {
        db.dao.ProductsMapper productsMapper = session.getMapper(db.dao.ProductsMapper.class);

        //ищем созданный продукт по ID
        db.model.Products selected = productsMapper.selectByPrimaryKey((long) id);

        //удаляем созданный продукт по ID
        productsMapper.deleteByPrimaryKey(selected.getId());
        session.commit();
    }

    @AfterAll
    static void afterAll() {
        session.close();
    }
}
