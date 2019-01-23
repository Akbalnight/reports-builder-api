package com.dias.services.reports.service;

import com.dias.services.reports.exception.ObjectNotFoundException;
import com.dias.services.reports.model.AbstractModel;
import com.dias.services.reports.repository.AbstractRepository;

import java.util.Optional;

public abstract class AbstractService<T extends AbstractModel> {

    protected abstract AbstractRepository<T> getRepository();

    /**
     * returns model
     * throws {@link ObjectNotFoundException} if not found
     */
    public T getById(Long id) throws ObjectNotFoundException {
        return Optional.ofNullable(getRepository().getById(id))
                .orElseThrow(() -> new ObjectNotFoundException(id));
    }

    public void create(T model) {
        getRepository().create(model);
    }

    public int delete(Long id) throws ObjectNotFoundException {
        checkObjectExists(id);
        return getRepository().delete(id);
    }

    private void checkObjectExists(Long id) throws ObjectNotFoundException {
        if (getById(id) == null) {
            throw new ObjectNotFoundException(id);
        }
    }

}
