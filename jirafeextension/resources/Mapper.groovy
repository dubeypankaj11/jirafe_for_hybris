import org.slf4j.LoggerFactory
import de.hybris.platform.core.PK
import de.hybris.platform.core.model.ItemModel
import org.jirafe.converter.JirafeJsonConverter

def getValue(ItemModel mmodel, Object expr) {
    def saveModel = model
    try {
        model = mmodel
        return expr()
    } finally {
        model = saveModel
    }
}

def toMap(ItemModel mmodel, Map map, Iterable keys=map.keySet()) {
    def log = LoggerFactory.getLogger(org.jirafe.converter.JirafeJsonConverter.class)

	log.debug('toMap: model = {}, keys = {}', mmodel, keys)
	def ret = _toMap(mmodel, map, keys)
	log.debug('toMap: returning {}', ret)
	return ret
}

def _toMap(ItemModel mmodel, Map map, Iterable keys=map.keySet()) {
    def log = LoggerFactory.getLogger(org.jirafe.converter.JirafeJsonConverter.class)
    
    def saveModel = model
    def ret = [:]

    context.add(String.format('%s<%s>',
                              modelService.getModelType(mmodel),
                              mmodel.pk))
    try {
        for (def key in keys) {
            context.add(key)
            def value = map[key]
            try {
                if (value instanceof Map) {
                    ret[key] = toMap(mmodel, value)
                }
                else if (value instanceof List) {
                    def prop = value[0]['_path']
                    ret[key] = []
                    mmodel[prop].each{
                        context.add(ret[key].size)
                        try {
                            ret[key].add(toMap(it, value[1]))
                        } catch (e) {
                            def error = context.join('.')
                            log.error("Data mapping exception: {}", error, e)
                            errors.add(error)
                        }
                        context.pop()
                    }
                    if (!ret[key]) ret[key] = null
                }
                else {
                    model = mmodel
                    ret[key] = value()
                }
            } catch (e) {
                def error = context.join('.')
                log.error("Data mapping exception: {}", error, e)
                errors.add(error)
            }
            context.pop()
        }
    } finally {
        model = saveModel
        context.pop()
    }
    return ret
}

def toMap (ItemModel mmodel, String type, Iterable keys=null) {
    Map map = jirafeJsonConverter.getDefinitionMap(this, type)
    return toMap(mmodel, map, keys ?: map.keySet())
}
