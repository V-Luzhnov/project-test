package hw6;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Backend Java. Homework 5
 *
 * @author Vitalii Luzhnov
 * @version 19.05.2022
 */
public class ChangeProductTest {
    static SqlSession session;

    static ProductService productService;
    Product product = null;
    int id = 1;
    String title;
    int price;
    String category;

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
                .withId(id)
                .withTitle(title)
                .withPrice(price)
                .withCategoryTitle(category);
    }

    @Test
    @Tag("Positive")
    @DisplayName("Change product (Positive)")
    void changeProductPositiveTest() throws IOException {
        Response<Product> response = productService.getProductById(id).execute();
        assertThat(response.isSuccessful(), CoreMatchers.is(true));
        assert response.body() != null;
        String titleOld = response.body().getTitle();
        int priceOld = response.body().getPrice();
        String categoryOld = response.body().getCategoryTitle();

        title = "TV set for Test";
        price = 12345;
        category = "Electronic";

        assertThat(title != titleOld, CoreMatchers.is(true));
        assertThat(price != priceOld, CoreMatchers.is(true));
        assertThat(category != categoryOld, CoreMatchers.is(true));

        setUp();
        response = productService.modifyProduct(product).execute();
        assertThat(response.isSuccessful(), CoreMatchers.is(true));
        assertThat(response.code(), equalTo(200));
        assert response.body() != null;
        assertThat(response.body().getTitle() != titleOld, is(true));
        assertThat(response.body().getPrice() != priceOld, is(true));
        assertThat(response.body().getCategoryTitle() != categoryOld, is(true));

        db.dao.ProductsMapper productsMapper = session.getMapper(db.dao.ProductsMapper.class);
        db.dao.CategoriesMapper categoriesMapper = session.getMapper(db.dao.CategoriesMapper.class);

        //ищем измененный продукт по ID
        db.model.Products selected = productsMapper.selectByPrimaryKey((long) response.body().getId());

        //ищем изменяемую категорию по наименованию
        db.model.CategoriesExample example = new db.model.CategoriesExample();
        example.createCriteria().andTitleLike(response.body().getCategoryTitle());
        List<db.model.Categories> list = categoriesMapper.selectByExample(example);
        db.model.Categories categories = list.get(0);
        Long category_id = categories.getId();

        //проверяем реквизиты созданного продукта
        assertThat(selected.getTitle(), equalTo(title));
        assertThat(selected.getPrice(), equalTo(price));
        assertThat(selected.getCategory_id(), equalTo(category_id));

        title = titleOld;
        price = priceOld;
        category = categoryOld;

        tearDown();
    }

    @Test
    @Tag("Negative")
    @DisplayName("Change product (Negative)")
    void changeProductNegativeTest() throws IOException {
        Response<Product> response = productService.getProductById(id).execute();
        assertThat(response.isSuccessful(), CoreMatchers.is(true));
        assert response.body() != null;
        title = response.body().getTitle();
        price = response.body().getPrice();
        category = "Auto";

        setUp();
        response = productService.modifyProduct(product).execute();
        assertThat(response.isSuccessful(), CoreMatchers.is(false));
        assertThat(response.code(), equalTo(500));
    }

    @SneakyThrows
    void tearDown() {
        db.dao.ProductsMapper productsMapper = session.getMapper(db.dao.ProductsMapper.class);
        db.dao.CategoriesMapper categoriesMapper = session.getMapper(db.dao.CategoriesMapper.class);

        //ищем измененный продукт по ID
        db.model.Products selected_p = productsMapper.selectByPrimaryKey((long) id);

        //ищем категорию измененного продукта по наименованию
        db.model.CategoriesExample example = new db.model.CategoriesExample();
        example.createCriteria().andTitleLike(category);
        List<db.model.Categories> list = categoriesMapper.selectByExample(example);
        db.model.Categories categories = list.get(0);
        Long category_id = categories.getId();

        //возвращаем измененные реквизиты
        selected_p.setTitle(title);
        selected_p.setPrice(price);
        selected_p.setCategory_id(category_id);
        productsMapper.updateByPrimaryKey(selected_p);
        session.commit();

        //проверяем изменение реквизитов
        assertThat(selected_p.getTitle(), equalTo(title));
        assertThat(selected_p.getPrice(), equalTo(price));
        assertThat(selected_p.getCategory_id(), equalTo(category_id));
    }

    @AfterAll
    static void afterAll() {
        session.close();
    }
}
