<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" omit-xml-declaration="yes" indent="no"/>

<xsl:template match="/">
   <xsl:apply-templates select="Raw"/>
</xsl:template>

<xsl:template match="Raw">
   <apm_order>
      <xsl:attribute name="apm_id"><xsl:value-of select="ApmID"/></xsl:attribute>
      <xsl:attribute name="apm_order_number"><xsl:value-of select="ApmOrderNumber"/></xsl:attribute>
      <xsl:attribute name="status">ordered</xsl:attribute>
      <xsl:attribute name="timestamp"><xsl:value-of select="Timestamp"/></xsl:attribute>
      <xsl:attribute name="apm_xml_version">2.0</xsl:attribute>
      <xsl:attribute name="brand"><xsl:value-of select="Brand"/></xsl:attribute>
      <xsl:attribute name="employee_id"></xsl:attribute>
      <xsl:attribute name="language">EN</xsl:attribute>
      <xsl:attribute name="currency_code">USD</xsl:attribute>
      <xsl:attribute name="currency_symbol">$</xsl:attribute>
      <xsl:attribute name="paid">true</xsl:attribute>
      <xsl:attribute name="grand_total">0.00</xsl:attribute>
      <xsl:attribute name="items_total">0.00</xsl:attribute>
      <xsl:attribute name="shipping_total">0.00</xsl:attribute>
      <xsl:attribute name="tax_total">0.00</xsl:attribute>
      <xsl:attribute name="service_fee">0.00</xsl:attribute>
      <xsl:attribute name="discount_total">0.00</xsl:attribute>
      <xsl:attribute name="order_discount">0.00</xsl:attribute>
      <xsl:attribute name="tax_rate">0.000000</xsl:attribute>
      <xsl:attribute name="tax_shipping">false</xsl:attribute>
      <xsl:attribute name="media_type">Harddrive</xsl:attribute>
      <xsl:attribute name="fulfillment_id">pickup</xsl:attribute>

      <shipment>
         <xsl:attribute name="shipment_id">1</xsl:attribute>
         <xsl:attribute name="delivery_method"></xsl:attribute>
         <xsl:attribute name="fname"><xsl:value-of select="FirstName"/></xsl:attribute>
         <xsl:attribute name="lname"><xsl:value-of select="LastName"/></xsl:attribute>
         <xsl:attribute name="title"></xsl:attribute>
         <xsl:attribute name="address1"><xsl:value-of select="Line1"/></xsl:attribute>
         <xsl:attribute name="address2"><xsl:value-of select="Line2"/></xsl:attribute>
         <xsl:attribute name="city"><xsl:value-of select="City"/></xsl:attribute>
         <xsl:attribute name="state"><xsl:value-of select="State"/></xsl:attribute>
         <xsl:attribute name="zip"><xsl:value-of select="Zip"/></xsl:attribute>
         <xsl:attribute name="country"><xsl:value-of select="Country"/></xsl:attribute>
         <xsl:attribute name="email"><xsl:value-of select="Email"/></xsl:attribute>
         <xsl:attribute name="phone"><xsl:value-of select="Phone"/></xsl:attribute>
         <xsl:attribute name="subtotal">0.00</xsl:attribute>
         <xsl:attribute name="subtotal_items">0.00</xsl:attribute>
         <xsl:attribute name="subtotal_discount">0.00</xsl:attribute>
         <xsl:attribute name="subtotal_shipping">0.00</xsl:attribute>
         <xsl:attribute name="subtotal_tax">0.00</xsl:attribute>
         <xsl:attribute name="fulfillment_type">pickup</xsl:attribute>

         <xsl:apply-templates select="Item"/>
      </shipment>

      <payment>
         <xsl:attribute name="payment_type">CASH</xsl:attribute>
         <xsl:attribute name="payment_amount">0.00</xsl:attribute>
         <xsl:attribute name="encrypted">false</xsl:attribute>
         <xsl:attribute name="tax_adjustment">0.00</xsl:attribute>
      </payment>

      <attributes>
         <xsl:if test="count(Method)>0">
            <xsl:attribute name="special_instructions"><xsl:value-of select="Method"/></xsl:attribute>
         </xsl:if>
         <xsl:attribute name="gifting">Ritz</xsl:attribute>
         <xsl:attribute name="finish"><xsl:value-of select="Finish"/></xsl:attribute>
      </attributes>

      <store>
         <xsl:attribute name="store_id">WEB</xsl:attribute>
      </store>

      <customer>
         <xsl:attribute name="fname"></xsl:attribute>
         <xsl:attribute name="lname"></xsl:attribute>
         <xsl:attribute name="title"></xsl:attribute>
         <xsl:attribute name="address1"></xsl:attribute>
         <xsl:attribute name="address2"></xsl:attribute>
         <xsl:attribute name="city"></xsl:attribute>
         <xsl:attribute name="state"></xsl:attribute>
         <xsl:attribute name="zip"></xsl:attribute>
         <xsl:attribute name="country"></xsl:attribute>
         <xsl:attribute name="email"></xsl:attribute>
         <xsl:attribute name="phone"></xsl:attribute>
      </customer>
   </apm_order>
</xsl:template>

<xsl:template match="Item">
   <order_item>
      <xsl:attribute name="product_type">
         <xsl:choose>
            <xsl:when test="count(ProductType)>0"><xsl:value-of select="ProductType"/></xsl:when>
            <xsl:otherwise>print</xsl:otherwise>
         </xsl:choose>
      </xsl:attribute>
      <xsl:if test="count(ProductSubType)>0">
         <xsl:attribute name="product_sub_type"><xsl:value-of select="ProductSubType"/></xsl:attribute>
      </xsl:if>
      <xsl:attribute name="product"><xsl:value-of select="Product"/></xsl:attribute>
      <xsl:attribute name="name"><xsl:value-of select="Name"/></xsl:attribute>
      <xsl:attribute name="description"><xsl:value-of select="Name"/></xsl:attribute>
      <xsl:attribute name="quantity"><xsl:value-of select="Quantity"/></xsl:attribute>
      <xsl:attribute name="price">0.00</xsl:attribute>
      <xsl:attribute name="line_item_total">0.00</xsl:attribute>
      <xsl:attribute name="line_item_discount">0.00</xsl:attribute>
      <xsl:attribute name="for_fulfillment">true</xsl:attribute>

      <xsl:if test="count(PageCount)>0">
         <attributes>
            <xsl:attribute name="page_count"><xsl:value-of select="PageCount"/></xsl:attribute>
         </attributes>
      </xsl:if>

      <xsl:apply-templates select="Image"/>
   </order_item>
</xsl:template>

<xsl:template match="Image">
   <image>
      <xsl:attribute name="path"><xsl:value-of select="Path"/></xsl:attribute>
      <xsl:attribute name="original_name"><xsl:value-of select="OriginalName"/></xsl:attribute>
      <xsl:attribute name="width"><xsl:value-of select="Width"/></xsl:attribute>
      <xsl:attribute name="height"><xsl:value-of select="Height"/></xsl:attribute>
      <xsl:attribute name="size"><xsl:value-of select="Size"/></xsl:attribute>
      <xsl:attribute name="res_override">false</xsl:attribute>
      <xsl:attribute name="redeye_removed">false</xsl:attribute>
      <xsl:attribute name="cropped">false</xsl:attribute>
      <xsl:attribute name="color_corrected">false</xsl:attribute>
      <xsl:attribute name="borders">false</xsl:attribute>
      <xsl:attribute name="fit_to_paper">false</xsl:attribute>
   </image>
</xsl:template>

</xsl:stylesheet>

