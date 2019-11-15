package nebula.plugin.responsible.gradle

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Namer
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import org.gradle.api.internal.MutationGuard
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil

class NamedContainerProperOrder<T> extends FactoryNamedDomainObjectContainer<T> {

    NamedContainerProperOrder(Class<T> type, Instantiator instantiator, NamedDomainObjectFactory<T> factory, CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        super(type, instantiator, factory, collectionCallbackActionDecorator)
    }

    @Deprecated
    NamedContainerProperOrder(Class<T> type, Instantiator instantiator, NamedDomainObjectFactory<T> factory) {
        super(type, instantiator, factory)
    }

    NamedContainerProperOrder(Class<T> type, Instantiator instantiator, Namer<? super T> namer, NamedDomainObjectFactory<T> factory, MutationGuard crossProjectConfiguratorMutationGuard, CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        super(type, instantiator, namer, factory, crossProjectConfiguratorMutationGuard, collectionCallbackActionDecorator)
    }

    NamedContainerProperOrder(Class<T> type, Instantiator instantiator, Closure factoryClosure, CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        super(type, instantiator, factoryClosure, collectionCallbackActionDecorator)
    }

    NamedContainerProperOrder(Class<T> type, Instantiator instantiator, Namer<? super T> namer, Closure factoryClosure, MutationGuard mutationGuard, CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        super(type, instantiator, namer, factoryClosure, mutationGuard, collectionCallbackActionDecorator)
    }

    @Override
    public T create(String name, Closure configureClosure) {
        assertCanAdd(name);
        T object = doCreate(name);
        // Configure the object BEFORE, adding and kicking off addEvents in doAdd
        ConfigureUtil.configure(configureClosure, object);
        add(object);
        return object;
    }

}
