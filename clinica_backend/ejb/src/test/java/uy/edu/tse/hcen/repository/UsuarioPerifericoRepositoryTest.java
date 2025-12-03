package uy.edu.tse.hcen.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.tse.hcen.model.UsuarioPeriferico;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioPerifericoRepositoryTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @Mock
    private TypedQuery<UsuarioPeriferico> typedQuery;

    @InjectMocks
    private UsuarioPerifericoRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        // Inyectar EntityManager mock usando reflection
        java.lang.reflect.Field emField = UsuarioPerifericoRepository.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(repository, em);
    }

    @Test
    void testFindByNickname() {
        String nickname = "testuser";
        UsuarioPeriferico usuario = new UsuarioPeriferico();
        usuario.setNickname(nickname);
        
        when(em.createQuery(eq("SELECT u FROM UsuarioPeriferico u WHERE u.nickname = :nickname"), eq(UsuarioPeriferico.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter("nickname", nickname)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(usuario);
        
        UsuarioPeriferico result = repository.findByNickname(nickname);
        
        assertNotNull(result);
        assertEquals(nickname, result.getNickname());
        verify(em, times(1)).createQuery(eq("SELECT u FROM UsuarioPeriferico u WHERE u.nickname = :nickname"), eq(UsuarioPeriferico.class));
        verify(typedQuery, times(1)).setParameter("nickname", nickname);
        verify(typedQuery, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameNotFound() {
        String nickname = "testuser";
        
        when(em.createQuery(eq("SELECT u FROM UsuarioPeriferico u WHERE u.nickname = :nickname"), eq(UsuarioPeriferico.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter("nickname", nickname)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new NoResultException());
        
        UsuarioPeriferico result = repository.findByNickname(nickname);
        
        assertNull(result);
        verify(em, times(1)).createQuery(eq("SELECT u FROM UsuarioPeriferico u WHERE u.nickname = :nickname"), eq(UsuarioPeriferico.class));
        verify(typedQuery, times(1)).setParameter("nickname", nickname);
        verify(typedQuery, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchema() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        Object[] row = createMockRow();
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNotNull(result);
        assertEquals(nickname, result.getNickname());
        assertEquals("PROFESIONAL", result.getRole());
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaWithIntegerId() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        Object[] row = createMockRow();
        row[0] = Integer.valueOf(1); // Integer ID
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaWithNullRole() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        Object[] row = createMockRow();
        row[3] = null; // role null
        row[8] = "ProfesionalSalud"; // dtype
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNotNull(result);
        assertEquals("PROFESIONAL", result.getRole()); // Debe deducirse del dtype
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaWithAdministradorDtype() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        Object[] row = createMockRow();
        row[3] = null; // role null
        row[8] = "AdministradorClinica"; // dtype
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNotNull(result);
        assertEquals("ADMINISTRADOR", result.getRole()); // Debe deducirse del dtype
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaException() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new RuntimeException("Database error"));
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNull(result);
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaNotFound() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNull(result);
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaWithBigIntegerId() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        Object[] row = createMockRow();
        row[0] = BigInteger.valueOf(1L); // BigInteger ID
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(em, times(1)).createNativeQuery(contains("schema_clinica_1"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameForLogin() {
        String nickname = "testuser";
        Object[] row = createMockRowForLogin();
        
        when(em.createNativeQuery(contains("public.usuarioperiferico"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameForLogin(nickname);
        
        assertNotNull(result);
        assertEquals(nickname, result.getNickname());
        assertEquals("101", result.getTenantId());
        verify(em, times(1)).createNativeQuery(contains("public.usuarioperiferico"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameForLoginNotFound() {
        String nickname = "testuser";
        
        when(em.createNativeQuery(contains("public.usuarioperiferico"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());
        
        UsuarioPeriferico result = repository.findByNicknameForLogin(nickname);
        
        assertNull(result);
        verify(em, times(1)).createNativeQuery(contains("public.usuarioperiferico"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameForLoginWithBigIntegerId() {
        String nickname = "testuser";
        Object[] row = createMockRowForLogin();
        row[0] = BigInteger.valueOf(1L);
        
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyInt(), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameForLogin(nickname);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(em).createNativeQuery(contains("public.usuarioperiferico"));
        verify(query).setParameter(1, nickname);
        verify(query).getSingleResult();
    }

    @Test
    void testFindByNicknameForLoginWithIntegerId() {
        String nickname = "testuser";
        Object[] row = createMockRowForLogin();
        row[0] = Integer.valueOf(1);
        
        when(em.createNativeQuery(contains("public.usuarioperiferico"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameForLogin(nickname);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(em, times(1)).createNativeQuery(contains("public.usuarioperiferico"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameForLoginException() {
        String nickname = "testuser";
        
        when(em.createNativeQuery(contains("public.usuarioperiferico"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new RuntimeException("Database error"));
        
        UsuarioPeriferico result = repository.findByNicknameForLogin(nickname);
        
        assertNull(result);
        verify(em, times(1)).createNativeQuery(contains("public.usuarioperiferico"));
        verify(query, times(1)).setParameter(1, nickname);
        verify(query, times(1)).getSingleResult();
    }

    @Test
    void testFindByNicknameInTenantSchemaWithBlankRole() {
        String nickname = "testuser";
        String schemaName = "schema_clinica_1";
        Object[] row = createMockRow();
        row[3] = ""; // role blank
        row[8] = "ProfesionalSalud"; // dtype
        
        when(em.createNativeQuery(contains("schema_clinica_1"))).thenReturn(query);
        when(query.setParameter(eq(1), eq(nickname))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(row);
        
        UsuarioPeriferico result = repository.findByNicknameInTenantSchema(nickname, schemaName);
        
        assertNotNull(result);
        assertEquals("PROFESIONAL", result.getRole()); // Debe deducirse del dtype
    }

    private Object[] createMockRow() {
        Object[] row = new Object[9];
        row[0] = 1L; // id
        row[1] = "testuser"; // nickname
        row[2] = "hashed_password"; // password_hash
        row[3] = "PROFESIONAL"; // role
        row[4] = "Nombre"; // nombre
        row[5] = "email@example.com"; // email
        row[6] = "MEDICINA_GENERAL"; // especialidad
        row[7] = "MONTEVIDEO"; // departamento
        row[8] = "ProfesionalSalud"; // dtype
        return row;
    }

    private Object[] createMockRowForLogin() {
        Object[] row = new Object[7];
        row[0] = 1L; // id
        row[1] = "testuser"; // nickname
        row[2] = "hashed_password"; // password_hash
        row[3] = "101"; // tenant_id
        row[4] = "PROFESIONAL"; // role
        row[5] = "Nombre"; // nombre
        row[6] = "email@example.com"; // email
        return row;
    }
}

