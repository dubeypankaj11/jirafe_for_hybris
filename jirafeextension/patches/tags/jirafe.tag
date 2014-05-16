<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:if test="${not empty jirafe2SiteId}">
<script type="text/javascript">
/* Jirafe */

<%-- Handle 4.8 style pageType --%>
<c:if test="${pageType['class'].name != 'java.lang.String'}">
    <c:set var="pageType" value="${pageType.value}"/>
</c:if>

<%-- Handle mixed case --%>
<c:set var="pageType" value="${fn:toUpperCase(pageType)}" />

(function(){
    var d=document,g=d.createElement('script'),s=d.getElementsByTagName('script')[0];
        g.type='text/javascript';g.defer=g.async=true;g.src=d.location.protocol+'//${jirafe2ApiUrl}';
        s.parentNode.insertBefore(g,s);
})();
var jirafe_site_id = "${jirafe2SiteId}";
var jirafe_org_id = "";

function jirafe_deferred(jirafe_api){
    var data = {
        "customer": {
            "firstname": "<spring:escapeBody javaScriptEscape="true">${user.firstName}</spring:escapeBody>",
            "lastname": "<spring:escapeBody javaScriptEscape="true">${user.lastName}</spring:escapeBody>",
            "id": "<spring:escapeBody javaScriptEscape="true">${user.uid}</spring:escapeBody>",
            "email": "<spring:escapeBody javaScriptEscape="true">${user.uid}</spring:escapeBody>"
        }
    };

    /* Custom Marketing Attribution

       You can pass in an array of url parameters to extract from the URL. Array position is important, 
       and will be interpreted left-to-right as least specific (highest-level attribution) to most specific. 

       For example...
       
       data.attribution = jirafe_parseAttribution(["atr1", "atr2", "atr3", "atr4"]);       

       ...will extract url parameters from...

       http://www.store.com?atr1=email&atr2=newsletter&atr3=2013-05&atr4=cta_button

       ...and set data.attribution to the following...

       data.attribution = ["email", "newsletter", "2013-05", "cta_button"];

    */
    data.attribution = jirafe_parseAttribution(["jirafe_atr1", "jirafe_atr2", "jirafe_atr3", "jirafe_atr4", "jirafe_atr5"]);

    <c:choose>
        <c:when test="${pageType == 'PRODUCTSEARCH'}">
            var type = "search";
            data.search = {
                "term": "<spring:escapeBody javaScriptEscape="true">${searchPageData.freeTextSearch}</spring:escapeBody>",
                "total_results": "${searchPageData.pagination.totalNumberOfResults}",
                "page": "${searchPageData.pagination.currentPage + 1}"
            };
        </c:when>

        <c:when test="${pageType == 'PRODUCT'}">
            var type = "product";
            data.product = {
                "product_code":  "<spring:escapeBody javaScriptEscape="true">${product.code}</spring:escapeBody>",
                "name": "<spring:escapeBody javaScriptEscape="true">${product.name}</spring:escapeBody>"
            };
        </c:when>

        <c:when test="${pageType == 'CATEGORY'}">
            var type="category";
            data.category = {
                "name": "<spring:escapeBody javaScriptEscape="true">${categoryName}</spring:escapeBody>"
            };
        </c:when>

        <c:when test="${pageType == 'CART'}">
            var type="cart";
        </c:when>

        <c:when test="${pageType == 'ORDERCONFIRMATION'}">
            jirafe_api.order.success(jirafe_org_id, jirafe_site_id, {
                "order": {
                    "num":  "<spring:escapeBody javaScriptEscape="true">${orderData.code}</spring:escapeBody>"
                }
            });
            var type="order_success";
        </c:when>

        <c:otherwise>
            var type = "other";
        </c:otherwise>
    </c:choose>

    jirafe_api.pageview(jirafe_org_id, jirafe_site_id, type, data);
}

</script>
</c:if>
