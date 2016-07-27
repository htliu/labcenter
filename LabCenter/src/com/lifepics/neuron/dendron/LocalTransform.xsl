<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" omit-xml-declaration="yes" indent="no"/>

<xsl:template match="/">
   <xsl:apply-templates select="Order"/>
</xsl:template>

<!-- an absent node, like a database null, means that a field is unspecified. -->
<!-- we want to preserve that information as we transform, hence all the ifs. -->

<xsl:template match="Order">
   <Order Version="2">
      <xsl:if test="count(OrderInfo/OrderDate)>0"><OrderTime><xsl:value-of select="OrderInfo/OrderDate"/></OrderTime></xsl:if>
      <Dealer>
         <xsl:if test="count(Merchant/Name)>0"><xsl:attribute name="Name"><xsl:value-of select="Merchant/Name"/></xsl:attribute></xsl:if>
         <xsl:if test="count(Merchant/Address1)>0"><Street1><xsl:value-of select="Merchant/Address1"/></Street1></xsl:if>
         <xsl:if test="count(Merchant/Address2)>0"><Street2><xsl:value-of select="Merchant/Address2"/></Street2></xsl:if>
         <xsl:if test="count(Merchant/City)>0"><City><xsl:value-of select="Merchant/City"/></City></xsl:if>
         <xsl:if test="count(Merchant/State)>0"><State><xsl:value-of select="Merchant/State"/></State></xsl:if>
         <xsl:if test="count(Merchant/PostalCode)>0"><ZIP><xsl:value-of select="Merchant/PostalCode"/></ZIP></xsl:if>
         <xsl:if test="count(OrderInfo/MerchantID)>0"><MerchantID><xsl:value-of select="OrderInfo/MerchantID"/></MerchantID></xsl:if>
      </Dealer>
      <Portal/><!-- included because invoice.xsl matches it -->
      <Customer>
         <xsl:if test="count(BillTo/Email)>0"><xsl:attribute name="ID"><xsl:value-of select="BillTo/Email"/></xsl:attribute></xsl:if>
         <xsl:if test="count(OrderInfo/CustomerID)>0"><xsl:attribute name="Number"><xsl:value-of select="OrderInfo/CustomerID"/></xsl:attribute></xsl:if>
         <xsl:if test="count(BillTo/FirstName)>0"><First><xsl:value-of select="BillTo/FirstName"/></First></xsl:if>
         <xsl:if test="count(BillTo/LastName)>0"><Last><xsl:value-of select="BillTo/LastName"/></Last></xsl:if>
         <BillTo>
            <xsl:if test="count(BillTo/FirstName)>0"><First><xsl:value-of select="BillTo/FirstName"/></First></xsl:if>
            <xsl:if test="count(BillTo/LastName)>0"><Last><xsl:value-of select="BillTo/LastName"/></Last></xsl:if>
            <xsl:if test="count(BillTo/Company)>0"><Company><xsl:value-of select="BillTo/Company"/></Company></xsl:if>
            <xsl:if test="count(BillTo/Address1)>0"><Street1><xsl:value-of select="BillTo/Address1"/></Street1></xsl:if>
            <xsl:if test="count(BillTo/Address2)>0"><Street2><xsl:value-of select="BillTo/Address2"/></Street2></xsl:if>
            <xsl:if test="count(BillTo/City)>0"><City><xsl:value-of select="BillTo/City"/></City></xsl:if>
            <xsl:if test="count(BillTo/State)>0"><State><xsl:value-of select="BillTo/State"/></State></xsl:if>
            <xsl:if test="count(BillTo/PostalCode)>0"><ZIP><xsl:value-of select="BillTo/PostalCode"/></ZIP></xsl:if>
            <xsl:if test="count(BillTo/Phone)>0"><Phone><xsl:value-of select="BillTo/Phone"/></Phone></xsl:if>
            <xsl:if test="count(BillTo/Country)>0"><Country><xsl:value-of select="BillTo/Country"/></Country></xsl:if>
         </BillTo>
         <ShipTo>
            <xsl:if test="count(ShipTo/FirstName)>0"><First><xsl:value-of select="ShipTo/FirstName"/></First></xsl:if>
            <xsl:if test="count(ShipTo/LastName)>0"><Last><xsl:value-of select="ShipTo/LastName"/></Last></xsl:if>
            <xsl:if test="count(ShipTo/Company)>0"><Company><xsl:value-of select="ShipTo/Company"/></Company></xsl:if>
            <xsl:if test="count(ShipTo/Address1)>0"><Street1><xsl:value-of select="ShipTo/Address1"/></Street1></xsl:if>
            <xsl:if test="count(ShipTo/Address2)>0"><Street2><xsl:value-of select="ShipTo/Address2"/></Street2></xsl:if>
            <xsl:if test="count(ShipTo/City)>0"><City><xsl:value-of select="ShipTo/City"/></City></xsl:if>
            <xsl:if test="count(ShipTo/State)>0"><State><xsl:value-of select="ShipTo/State"/></State></xsl:if>
            <xsl:if test="count(ShipTo/PostalCode)>0"><ZIP><xsl:value-of select="ShipTo/PostalCode"/></ZIP></xsl:if>
            <xsl:if test="count(ShipTo/Phone)>0"><Phone><xsl:value-of select="ShipTo/Phone"/></Phone></xsl:if>
            <xsl:if test="count(ShipTo/Country)>0"><Country><xsl:value-of select="ShipTo/Country"/></Country></xsl:if>
            <xsl:if test="count(ShipTo/Comment)>0"><Comment><xsl:value-of select="ShipTo/Comment"/></Comment></xsl:if>
         </ShipTo>
      </Customer>
      <SpecialInstructions>
         <xsl:if test="count(OrderInfo/SpecialInstructions)>0"><Message><xsl:value-of select="OrderInfo/SpecialInstructions"/></Message></xsl:if>
      </SpecialInstructions>
      <Payment>
         <xsl:if test="count(Payment/PaymentType)>0">
            <Type>
               <!-- two of the enum values are represented differently in order.xml -->
               <xsl:choose>
                  <xsl:when test="Payment/PaymentType='PD'">Pay In Store</xsl:when>
                  <xsl:when test="Payment/PaymentType='OA'">On Account</xsl:when>
                  <xsl:otherwise><xsl:value-of select="Payment/PaymentType"/></xsl:otherwise>
               </xsl:choose>
            </Type>
         </xsl:if>
         <xsl:if test="count(Payment/CardType)>0"><CardType><xsl:value-of select="Payment/CardType"/></CardType></xsl:if>
         <xsl:if test="count(Payment/CardNumber)>0"><Account><xsl:value-of select="Payment/CardNumber"/></Account></xsl:if>
         <xsl:if test="count(Payment/ExpirationDate)>0"><Expiration><xsl:value-of select="Payment/ExpirationDate"/></Expiration></xsl:if>
         <xsl:if test="count(Payment/SecurityCode)>0"><Authorization><xsl:value-of select="Payment/SecurityCode"/></Authorization></xsl:if>
      </Payment>
      <OrderSummary/><!-- placeholder for postprocessing -->
      <Items>
         <xsl:apply-templates select="OrderItem"/>
      </Items>
      <Addons/><!-- included because invoice.xsl matches it -->
      <xsl:if test="count(OrderInfo/Subtotal)>0"><SubTotal><xsl:value-of select="OrderInfo/Subtotal"/></SubTotal></xsl:if>
      <xsl:if test="count(OrderInfo/TaxRate)>0"><TaxRate><xsl:value-of select="OrderInfo/TaxRate"/></TaxRate></xsl:if>
      <xsl:if test="count(OrderInfo/Tax)>0"><Tax><xsl:value-of select="OrderInfo/Tax"/></Tax></xsl:if>
      <xsl:if test="count(OrderInfo/ShippingCharge)>0"><Shipping><xsl:value-of select="OrderInfo/ShippingCharge"/></Shipping></xsl:if>
      <Discount>
         <xsl:if test="count(OrderInfo/Discount/FreePrints)>0"><FreePrints><xsl:value-of select="OrderInfo/Discount/FreePrints"/></FreePrints></xsl:if>
         <xsl:if test="count(OrderInfo/Discount/Credit)>0"><Credit><xsl:value-of select="OrderInfo/Discount/Credit"/></Credit></xsl:if>
         <xsl:if test="count(OrderInfo/Discount/PercentDiscount)>0"><PercentDiscount><xsl:value-of select="OrderInfo/Discount/PercentDiscount"/></PercentDiscount></xsl:if>
         <xsl:if test="count(OrderInfo/Discount/PermanentDiscount)>0"><PermanentDiscount><xsl:value-of select="OrderInfo/Discount/PermanentDiscount"/></PermanentDiscount></xsl:if>
      </Discount>
      <xsl:if test="count(OrderInfo/Total)>0"><GrandTotal><xsl:value-of select="OrderInfo/Total"/></GrandTotal></xsl:if>
   </Order>
</xsl:template>

<xsl:template match="OrderItem">
   <Item>
      <xsl:attribute name="ID"><xsl:value-of select="position()"/></xsl:attribute>
      <xsl:if test="count(Images)>0">
         <Images>
            <xsl:for-each select="Images/File">
               <Image>
                  <ImageURL><xsl:value-of select="."/></ImageURL>
                  <OrigImageName><xsl:value-of select="."/></OrigImageName>
                  <ImageStreamSize/><!-- placeholder for postprocessing -->
                  <ImageDescription><xsl:value-of select="."/></ImageDescription>
               </Image>
            </xsl:for-each>
         </Images>
      </xsl:if>
      <xsl:if test="count(ProductCode)>0"><ProductSku><xsl:value-of select="ProductCode"/></ProductSku></xsl:if>
      <xsl:if test="count(Attributes)>0">
         <Attributes>
            <xsl:for-each select="Attributes/Attribute">
               <Attribute>
                  <xsl:if test="count(GroupName)>0"><GroupName><xsl:value-of select="GroupName"/></GroupName></xsl:if>
                  <xsl:if test="count(Value)>0"><Value><xsl:value-of select="Value"/></Value></xsl:if>
               </Attribute>
            </xsl:for-each>
         </Attributes>
      </xsl:if>
      <xsl:if test="count(ProductDescription)>0"><ProductDescription><xsl:value-of select="ProductDescription"/></ProductDescription></xsl:if>
      <xsl:if test="count(Quantity)>0">
         <Qty>
            <xsl:choose>
               <xsl:when test="count(ProductionQuantity)>0"><xsl:value-of select="ProductionQuantity"/></xsl:when>
               <xsl:otherwise><xsl:value-of select="Quantity"/></xsl:otherwise>
            </xsl:choose>
         </Qty>
         <DisplayQty><xsl:value-of select="Quantity"/></DisplayQty>
      </xsl:if>
      <!-- else don't send anything, even if ProductionQuantity is set -->
      <!-- always produce an IsCD node since there's no PageCount here -->
      <IsCD>
         <xsl:choose>
            <xsl:when test="CDProduct='True'">true</xsl:when>
            <xsl:otherwise>false</xsl:otherwise>
         </xsl:choose>
      </IsCD>
      <xsl:if test="count(Comments)>0"><Comments><xsl:value-of select="Comments"/></Comments></xsl:if>
      <xsl:if test="count(Price)>0"><Price><xsl:value-of select="Price"/></Price></xsl:if>
      <xsl:if test="count(Total)>0"><Total><xsl:value-of select="Total"/></Total></xsl:if>
   </Item>
</xsl:template>

</xsl:stylesheet>

