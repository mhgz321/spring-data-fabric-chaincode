package io.github.hooj0.springdata.fabric.chaincode.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import io.github.hooj0.springdata.fabric.chaincode.annotations.Entity;
import io.github.hooj0.springdata.fabric.chaincode.core.ChaincodeOperations;
import io.github.hooj0.springdata.fabric.chaincode.core.ChaincodeTemplate;
import io.github.hooj0.springdata.fabric.chaincode.core.convert.ChaincodeCustomConversions;
import io.github.hooj0.springdata.fabric.chaincode.core.convert.MappingChaincodeConverter;
import io.github.hooj0.springdata.fabric.chaincode.core.mapping.SimpleChaincodeMappingContext;

/**
 * <b>function:</b>
 * @author hoojo
 * @createDate 2018年7月18日 下午5:42:32
 * @file ChaincodeRepositoryConfigExtension.java
 * @package io.github.hooj0.springdata.fabric.chaincode.repository.config
 * @project spring-data-fabric-chaincode
 * @blog http://hoojo.cnblogs.com
 * @email hoojo_@126.com
 * @version 1.0
 */
public class ChaincodeRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final String CHAINCODE_TEMPLATE_REF = "chaincode-template-ref";
	
	enum BeanDefinitionName {
		CHAINCODE_MAPPTING_CONTEXT("chaincodeMappingContext"), 
		CHAINCODE_OPERATIONS("chaincodeOperations"), 
		CHAINCODE_TEMPLATE("chaincodeTemplate"), 
		CHAINCODE_CONVERTER("chaincodeConverter"), 
		CHAINCODE_CLIENT("chaincodeClient"), 
		CUSTOM_CONVERSIONS("customConversions");
		
		String beanName;
		BeanDefinitionName(String beanName) {
			this.beanName = beanName;
		}

		public String getBeanName() {
			return beanName;
		}
	}
	
	@Override
	public String getRepositoryFactoryBeanClassName() {
		return null;
	}

	@Override
	public String getModuleName() {
		return "FabricChaincode";
	}
	
	@Override
	protected String getModulePrefix() {
		return "chaincode";
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {
		Element element = config.getElement();

		String templateRef = Optional.ofNullable(element.getAttribute(CHAINCODE_TEMPLATE_REF)) //
				.filter(StringUtils::hasText) //
				.orElse(BeanDefinitionName.CHAINCODE_OPERATIONS.beanName);

		builder.addPropertyReference(BeanDefinitionName.CHAINCODE_OPERATIONS.getBeanName(), templateRef);
		//builder.addPropertyReference(BeanDefinitionName.CHAINCODE_CLIENT.getBeanName(), BeanDefinitionName.CHAINCODE_CLIENT.beanName);
		builder.addPropertyReference(BeanDefinitionName.CHAINCODE_CONVERTER.getBeanName(), BeanDefinitionName.CHAINCODE_CONVERTER.beanName);
		builder.addPropertyReference(BeanDefinitionName.CHAINCODE_MAPPTING_CONTEXT.getBeanName(), BeanDefinitionName.CHAINCODE_MAPPTING_CONTEXT.beanName);
		builder.addPropertyReference(BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName(), BeanDefinitionName.CUSTOM_CONVERSIONS.beanName);
	}
	
	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
		AnnotationAttributes attrs = config.getAttributes();

		if (StringUtils.hasText(attrs.getString(CHAINCODE_TEMPLATE_REF))) {
			builder.addPropertyReference(BeanDefinitionName.CHAINCODE_OPERATIONS.getBeanName(), attrs.getString(CHAINCODE_TEMPLATE_REF));
		} else {
			builder.addPropertyReference(BeanDefinitionName.CHAINCODE_OPERATIONS.getBeanName(), BeanDefinitionName.CHAINCODE_OPERATIONS.beanName);
		}
		
		//builder.addPropertyReference(BeanDefinitionName.CHAINCODE_CLIENT.getBeanName(), BeanDefinitionName.CHAINCODE_CLIENT.beanName);
		builder.addPropertyReference(BeanDefinitionName.CHAINCODE_CONVERTER.getBeanName(), BeanDefinitionName.CHAINCODE_CONVERTER.beanName);
		builder.addPropertyReference(BeanDefinitionName.CHAINCODE_MAPPTING_CONTEXT.getBeanName(), BeanDefinitionName.CHAINCODE_MAPPTING_CONTEXT.beanName);
		builder.addPropertyReference(BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName(), BeanDefinitionName.CUSTOM_CONVERSIONS.beanName);
	}

	/**
	 * 在评估存储分配的存储库接口时，返回注释以扫描域类型。 模块应返回标识明确由repo管理的域类型的注释。
	 * @author hoojo
	 * @createDate 2018年7月10日 上午10:53:11
	 */
	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.singleton(Entity.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(ChaincodeTemplate.class);
	}

	@Override
	protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
		return !metadata.isReactiveRepository();
	}
	
	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {
		super.registerBeansForRoot(registry, configurationSource);

		registeCustomConversionsIfNotPresent(registry, configurationSource);
		registerMappingContextIfNotPresent(registry, configurationSource);
		registerConverterIfNotPresent(registry, configurationSource);
		registerTemplateIfNotPresent(registry, configurationSource);
	}
	
	private void registeCustomConversionsIfNotPresent(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(ChaincodeCustomConversions.class);

		definition.getConstructorArgumentValues().addGenericArgumentValue(Collections.emptyList());
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName(), definition);
	}
	
	private void registerMappingContextIfNotPresent(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(SimpleChaincodeMappingContext.class);
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinitionName.CHAINCODE_MAPPTING_CONTEXT.getBeanName(), definition);
	}

	private void registerConverterIfNotPresent(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(MappingChaincodeConverter.class);
		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
		ctorArgs.addIndexedArgumentValue(0, new RuntimeBeanReference(BeanDefinitionName.CHAINCODE_MAPPTING_CONTEXT.getBeanName()));
		definition.setConstructorArgumentValues(ctorArgs);

		MutablePropertyValues properties = new MutablePropertyValues();
		properties.add("customConversions", new RuntimeBeanReference(BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName()));
		definition.setPropertyValues(properties);

		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinitionName.CHAINCODE_CONVERTER.getBeanName(), definition);
	}

	private void registerTemplateIfNotPresent(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition beanDefinition = new RootBeanDefinition(ChaincodeTemplate.class);
		beanDefinition.setTargetType(ChaincodeOperations.class);

		//ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();

		//ctorArgs.addIndexedArgumentValue(0, createHttpClientFactory());
		//ctorArgs.addIndexedArgumentValue(1, new RuntimeBeanReference(BeanDefinitionName.CHAINCODE_CONVERTER.getBeanName()));

		//beanDefinition.setConstructorArgumentValues(ctorArgs);

		registerIfNotAlreadyRegistered(beanDefinition, registry, BeanDefinitionName.CHAINCODE_TEMPLATE.getBeanName(), beanDefinition);
	}

	@SuppressWarnings("unused")
	private BeanDefinition createClientFactory() {

		GenericBeanDefinition clientFactory = new GenericBeanDefinition();
		// clientFactory.setBeanClass(HttpClientFactory.class);
		
		ConstructorArgumentValues args = new ConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(BeanDefinitionName.CHAINCODE_CLIENT.getBeanName()));
		clientFactory.setConstructorArgumentValues(args);
		
		return clientFactory;
	}
}