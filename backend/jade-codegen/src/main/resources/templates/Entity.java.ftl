package ${package}.${module}.entity;

<#if columns?filter(c -> c.javaType == "OffsetDateTime")?size != 0>
import java.time.OffsetDateTime;
</#if>
<#if columns?filter(c -> c.javaType == "BigDecimal")?size != 0>
import java.math.BigDecimal;
</#if>
<#if columns?filter(c -> c.javaType == "LocalDate")?size != 0>
import java.time.LocalDate;
</#if>
<#if columns?filter(c -> c.javaType == "UUID")?size != 0>
import java.util.UUID;
</#if>
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
<#if columns?filter(c -> c.name == "created_at")?size != 0 || columns?filter(c -> c.name == "updated_at")?size != 0>
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
</#if>

/**
 * ${table.comment!className}（自动生成）
 * @author ${author}
 * @date ${date?string("yyyy-MM-dd")}
 */
@Data
@Entity
@Table(name = "${table.name}")
public class ${className} extends PanacheEntityBase {

<#list columns as col>
    /**
     * ${col.comment!col.name}
     */
    <#if col.primaryKey>
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    </#if>
    <#if !col.nullable && !col.primaryKey>
    @Column(nullable = false<#if col.length?? && col.length != 0>, length = ${col.length}</#if>)
    <#elseIf col.length?? && col.length != 0>
    @Column(length = ${col.length})
    </#if>
    <#if col.name == "created_at">
    @CreationTimestamp
    </#if>
    <#if col.name == "updated_at">
    @UpdateTimestamp
    </#if>
    public ${col.javaType} ${SchemaReader.toCamelCase(col.name)};

</#list>
}
