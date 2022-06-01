package hw6;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Backend Java. Homework 6
 *
 * @author Vitalii Luzhnov
 * @version 31.05.2022
 */
public class CreateCategoryTest {
    static SqlSession session;

    String category;

    @BeforeAll
    static void beforeAll() throws IOException {
        session = null;
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        session = sqlSessionFactory.openSession();
    }

    @Test
    @Tag("Positive")
    @DisplayName("Category creation (Positive)")
    void createCategoryPositiveTest() {

        category = "test";

        db.dao.CategoriesMapper categoriesMapper = session.getMapper(db.dao.CategoriesMapper.class);

        //ищем каегорию по наименованию
        db.model.CategoriesExample categoriesExample = new db.model.CategoriesExample();
        categoriesExample.createCriteria().andTitleLike(category);
        List<db.model.Categories> list = categoriesMapper.selectByExample(categoriesExample);

        //проверяем отсутствие категорий
        assertThat(list.size(), equalTo(0));

        //создаем категорию
        db.model.Categories categories = new db.model.Categories();
        categories.setTitle(category);
        categoriesMapper.insert(categories);
        session.commit();

        //находим созданную категорию
        categoriesExample = new db.model.CategoriesExample();
        categoriesExample.createCriteria().andTitleLike(category);
        list = categoriesMapper.selectByExample(categoriesExample);
        db.model.Categories selected = list.get(0);

        //проверяем наименование
        assertThat(selected.getTitle(), equalTo(category));

        //удаляем созданную категорию
        categoriesMapper.deleteByPrimaryKey(selected.getId());
        session.commit();
    }

    @AfterAll
    static void afterAll() {
        session.close();
    }
}
