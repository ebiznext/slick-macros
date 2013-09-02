package slickmacros.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;


public class EmfUtils {

	/**
	 * Instantiate EcoreFactory
	 */
	public static EcoreFactory theCoreFactory = EcoreFactory.eINSTANCE;

	/**
	 * Create EClass instance by name
	 * 
	 * @param name
	 *            EClass name
	 * @return
	 */
	static public EClass createEClass(String name) {
		EClass eclass = theCoreFactory.createEClass();
		eclass.setName(name);
		return eclass;
	}

	static public EcorePackage getCorePackage() {
		return EcorePackage.eINSTANCE;
	}

	/**
	 * Create EParameter by name and type
	 * 
	 * @param name
	 *            EParameter
	 * @param eDataType
	 *            EParameter data type
	 * @return
	 */
	static public EParameter createEParameter(String name, EClassifier eDataType) {
		EParameter eParameter = theCoreFactory.createEParameter();
		eParameter.setName(name);
		eParameter.setEType(eDataType);
		return eParameter;
	}

	/**
	 * Create attribute for EClass
	 * 
	 * @param name
	 *            EAttribute name
	 * @param eType
	 *            EAttribute data type
	 * @param eClass
	 *            EClass instance
	 */
	static public void addEAttributeToEClass(String name, EClassifier eType,
			EClass eClass) {
		EAttribute eAttribute = theCoreFactory.createEAttribute();
		eAttribute.setName(name);
		eAttribute.setEType(eType);
		eClass.getEStructuralFeatures().add(eAttribute);
	}

	/**
	 * Create attributes for EClass
	 * 
	 * @param name
	 *            EAttribute name
	 * @param eType
	 *            EAttribute data type
	 * @param eClass
	 *            EClass instance
	 */
	static public void addOprionalEAttributeToEClass(String name,
			EClassifier eType, EClass eClass) {
		EAttribute eAttribute = theCoreFactory.createEAttribute();
		eAttribute.setName(name);
		eAttribute.setEType(eType);
		eAttribute.setLowerBound(0);
		eAttribute.setUpperBound(1);
		eClass.getEStructuralFeatures().add(eAttribute);
	}

	/**
	 * Create one to many relationship to EClass
	 * 
	 * @param name
	 *            EReference name
	 * @param eType
	 *            EReference data type
	 * @param eClass
	 */
	static public void addOne2ManyEReferenceToEClass(String name,
			EClassifier eType, EClass eClass) {
		EReference eReference = theCoreFactory.createEReference();
		eReference.setName(name);
		eReference.setEType(eType);
		eReference.setUpperBound(EStructuralFeature.UNBOUNDED_MULTIPLICITY);
		eReference.setContainment(false);
		eClass.getEStructuralFeatures().add(eReference);
	}

	/**
	 * Create one to one relationship to EClass
	 * 
	 * @param name
	 *            EReference name
	 * @param eType
	 *            EReference data type
	 * @param eClass
	 */
	static public void addOne2OneEReferenceToEClass(String name,
			EClassifier eType, EClass eClass) {
		EReference eReference = theCoreFactory.createEReference();
		eReference.setName(name);
		eReference.setEType(eType);
		eReference.setUpperBound(1);
		eReference.setLowerBound(1);
		eReference.setContainment(false);
		eClass.getEStructuralFeatures().add(eReference);
	}

	/**
	 * Create optional one to one (zero or one) relationship to EClass
	 * 
	 * @param name
	 *            EReference name
	 * @param eType
	 *            EReference data type
	 * @param eClass
	 */
	static public void addOptionalOne2OneEReferenceToEClass(String name,
			EClassifier eType, EClass eClass) {
		EReference eReference = theCoreFactory.createEReference();
		eReference.setName(name);
		eReference.setEType(eType);
		eReference.setLowerBound(0);
		eReference.setUpperBound(1);
		eReference.setContainment(false);
		eClass.getEStructuralFeatures().add(eReference);
	}

	/**
	 * Create data type by name
	 * 
	 * @param name
	 * @return
	 */
	static public EClassifier createCustomDataType(String name) {
		EClassifier eClassifier = theCoreFactory.createEDataType();
		eClassifier.setName(name);
		return eClassifier;
	}

	/**
	 * make bidirectional relationship between eReference1 and eReference2
	 * 
	 * @param eReference1
	 * @param eReference2
	 */
	static public void makeBidirectionalEReference(EReference eReference1,
			EReference eReference2) {
		eReference1.setEOpposite(eReference2);
		eReference2.setEOpposite(eReference1);
	}

	/**
	 * Create operation for EClass
	 * 
	 * @param name
	 *            EOperation name
	 * @param eParameters
	 *            EOperation parameters
	 * @param eClass
	 */
	static public void addEOperationToEClass(String name,
			EClassifier eReturnType, List<EParameter> parameters, EClass eClass) {
		Collection<EParameter> c = new ArrayList<EParameter>();
		c = parameters;
		EOperation eOperation = theCoreFactory.createEOperation();
		eOperation.setName(name);
		eOperation.setEType(eReturnType);
		eOperation.getEParameters().addAll(c);
		eClass.getEOperations().add(eOperation);
	}

	/**
	 * Instantiate EPackage and provide unique URI to identify this package
	 * 
	 * @param name
	 *            EPackage
	 * @return EPackage
	 */
	static public EPackage createEPackage(String name) {
		EPackage ePackage = theCoreFactory.createEPackage();
		ePackage.setName(name);
		ePackage.setNsPrefix(name); // It will be used in an XMI serialization
		ePackage.setNsURI(name); // It will be used in an XMI serialization
		return ePackage;
	}

	/**
	 * add eClass to ePackage
	 * 
	 * @param eclassifier
	 * @param ePackage
	 */
	static public void addEClassToEPackage(EClassifier eclassifier,
			EPackage ePackage) {
		ePackage.getEClassifiers().add(eclassifier);
	}

	/**
	 * Serialize model instance
	 * 
	 * @param ePackages
	 * @param eCoreOutputFile
	 */
	static public void savePackages(Collection<EPackage> ePackages,
			String eCodeOutputFile) {
		ResourceSet resourceSet = new ResourceSetImpl();
		/*
		 * Register XML Factory implementation using DEFAULT_EXTENSION
		 */
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("*", new XMLResourceFactoryImpl());

		/*
		 * Create empty resource with the given URI
		 */
		Resource resource = resourceSet.createResource(URI
				.createURI(eCodeOutputFile));

		/*
		 * Add bookStoreObject to contents list of the resource
		 */
		resource.getContents().addAll(ePackages);

		try {
			/*
			 * Save the resource
			 */
			resource.save(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Serialize single package
	 * 
	 * @param Package
	 * @param eCoreOutputFile
	 */
	static public void savePackage(EPackage ePackage, String eCodeOutputFile) {
		ResourceSet resourceSet = new ResourceSetImpl();
		/*
		 * Register XML Factory implementation using DEFAULT_EXTENSION
		 */
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("*", new XMLResourceFactoryImpl());

		/*
		 * Create empty resource with the given URI
		 */
		Resource resource = resourceSet.createResource(URI
				.createURI(eCodeOutputFile));

		/*
		 * Add bookStoreObject to contents list of the resource
		 */
		resource.getContents().add(ePackage);

		try {
			/*
			 * Save the resource
			 */
			resource.save(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
