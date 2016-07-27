<xsl:stylesheet id="invoice-LabCenter-v48" version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" version="3.2" media-type="text/html" omit-xml-declaration="yes" indent="yes"/>

<xsl:param name="label" select="false()"/>
<xsl:param name="home"  select="true()"/>
<xsl:param name="shipmethod"/>
<xsl:param name="product"/>
<xsl:param name="accountsummary" select="false()"/>
<xsl:param name="timestamp" select="Order/OrderTime"/>
<xsl:param name="lifepicsID"/>
<xsl:param name="items" select="true()"/>
<xsl:param name="color" select="true()"/>
<xsl:param name="disposition" select="false()"/>
<xsl:param name="enlargetype" select="false()"/>
<xsl:param name="shownumber" select="true()"/>
<xsl:param name="override" select="false()"/>
<xsl:param name="ovname"/>
<xsl:param name="ovstreet1"/>
<xsl:param name="ovstreet2"/>
<xsl:param name="ovcity"/>
<xsl:param name="ovstate"/>
<xsl:param name="ovzip"/>
<xsl:param name="markshipvalue"/>
<xsl:param name="markpay" select="false()"/>
<xsl:param name="includetax" select="false()"/>
<xsl:param name="addmessage"/>
<xsl:param name="markpro" select="false()"/>
<xsl:param name="markproID"/>
<xsl:param name="markprevalue"/>
<xsl:param name="showpickup" select="false()"/>
<xsl:param name="logo"/>
<xsl:param name="logowidth"/>
<xsl:param name="logoheight"/>

<xsl:variable name="currency">
   <xsl:choose>
      <xsl:when test="count(Order/Currency)>0">
         <xsl:value-of select="Order/Currency"/>
      </xsl:when>
      <xsl:otherwise>$</xsl:otherwise>
   </xsl:choose>
</xsl:variable>

<xsl:template match="/">
   <xsl:apply-templates select="Order"/>
</xsl:template>

<xsl:template match="Order">
   <xsl:choose>
      <xsl:when test="$label">

         <html>
            <body>
               <xsl:choose>
                  <xsl:when test="string-length($product)!=0">
                     <xsl:call-template name="LabelProduct"/>
                  </xsl:when>
                  <xsl:when test="$home">
                     <xsl:call-template name="LabelHome"/>
                  </xsl:when>
                  <xsl:when test="$accountsummary">
                     <xsl:call-template name="LabelSummary"/>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:call-template name="LabelAccount"/>
                  </xsl:otherwise>
               </xsl:choose>
            </body>
         </html>

      </xsl:when>
      <xsl:otherwise>

         <html>
            <head>
               <title><xsl:value-of select="Dealer/@Name"/> Invoice for Order <xsl:value-of select="@ID"/></title>
            </head>
            <body>
               <xsl:call-template name="Invoice"/>
            </body>
         </html>

      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template name="LabelProduct">
   <table border="0" cellspacing="0" cellpadding="0">
      <tr valign="top">
         <td width="200">
            <font size="+2"><strong>
               <xsl:value-of select="Customer/BillTo/First"/>
               <xsl:text> </xsl:text>
               <xsl:value-of select="Customer/BillTo/Last"/>
            </strong></font><br/>
            <xsl:value-of select="Customer/@ID"/><br/>
            <xsl:value-of select="Customer/BillTo/Phone"/>
         </td>
         <td width="200">
            <xsl:for-each select="OrderSummary[ShipMethod=$shipmethod]">
               <xsl:for-each select="Product[Description=$product]">
                  <b>( <xsl:value-of select="count(preceding-sibling::Product)+1"/> / <xsl:value-of select="count(../Product)"/> ) </b>
                  <xsl:value-of select="$product"/><br/> <!-- Description -->
                  Quantity: <xsl:call-template name="QtyUC"/><br/>
                  <font size="+1">Barcode Total: <xsl:value-of select="$currency"/><xsl:value-of select="Subtotal"/></font>
               </xsl:for-each>
            </xsl:for-each>
         </td>
      </tr>
      <tr valign="bottom">
         <td width="200">
            <font size="+2">
               <xsl:call-template name="OrderID"/>
            </font><br/>
            <font size="+1">
               <xsl:value-of select="$timestamp"/>
            </font>
         </td>
         <td width="200">
            <xsl:for-each select="OrderSummary[ShipMethod=$shipmethod]">
               <xsl:for-each select="Product[Description=$product]">
                  <table cellpadding="0" cellspacing="0" border="0">
                     <tr>
                        <td width="0" height="40"></td>
                        <xsl:apply-templates select="Barcode"/>
                     </tr>
                  </table>
                  <xsl:value-of select="Barcode/d"/>
               </xsl:for-each>
            </xsl:for-each>
         </td>
      </tr>
   </table>
</xsl:template>

<xsl:template name="LabelHome">
   <table border="0" cellspacing="0" cellpadding="0">
      <tr valign="top">
         <td><font size="-1">
            <xsl:if test="not($override)">
               <xsl:for-each select="Dealer">
                  <xsl:value-of select="@Name"/><br/>
                  <xsl:call-template name="DealerAddress"/><br/>
               </xsl:for-each>
            </xsl:if>
            <xsl:if test="$override">
               <xsl:value-of select="$ovname"/><br/>
               <xsl:call-template name="OverrideAddress"/><br/>
            </xsl:if>
            <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
         </font></td>
         <xsl:if test="string-length($markshipvalue)!=0">
            <td>
               <table border="0" cellspacing="0" cellpadding="0">
                  <tr><td bgcolor="#000000">
                     <font size="+2" color="#FFFFFF"><b>
                        <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
                        <xsl:value-of select="$markshipvalue"/>
                        <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
                     </b></font>
                  </td></tr>
               </table>
            </td>
         </xsl:if>
      </tr>
      <tr valign="top">
         <td width="200"><font size="-1">
            <xsl:if test="$shownumber">
               <xsl:call-template name="OrderID"/>
            </xsl:if>
         </font></td>
         <td><b><font size="+1">
            <xsl:for-each select="Customer/ShipTo">
               <xsl:call-template name="CustomerName"/><br/>
               <xsl:call-template name="CustomerAddress"/>
            </xsl:for-each>
         </font></b></td>
      </tr>
   </table>
</xsl:template>

<xsl:template name="LabelSummary">
   <table border="0" cellspacing="0" cellpadding="0">

      <xsl:if test="string-length(OrderSource)!=0">
         <tr><td colspan="2" align="center" bgcolor="#000000">
            <font size="+2" color="#FFFFFF"><strong>
               <xsl:value-of select="OrderSource"/>
            </strong></font>
         </td></tr>
      </xsl:if>

      <tr valign="top">
         <td width="200">
            <font size="+2"><strong>
               <xsl:value-of select="Customer/BillTo/First"/>
               <xsl:text> </xsl:text>
               <xsl:value-of select="Customer/BillTo/Last"/>
            </strong></font><br/>
            <xsl:value-of select="Customer/@ID"/><br/>
            <xsl:value-of select="Customer/BillTo/Phone"/><br/>

            <!-- if the XML doesn't have any ShipMethod, LC will still send in -->
            <!-- the Comment field as shipmethod, but we can't condition on it -->
            <xsl:choose>
               <xsl:when test="count(OrderSummary/ShipMethod)>0">
                  <xsl:for-each select="OrderSummary[ShipMethod=$shipmethod]">
                     <xsl:call-template name="SummaryBlock"/>
                  </xsl:for-each>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:for-each select="OrderSummary">
                     <xsl:call-template name="SummaryBlock"/>
                  </xsl:for-each>
               </xsl:otherwise>
            </xsl:choose>

            <font size="+2">
               <xsl:call-template name="OrderID"/>
            </font><br/>
            <font size="+1">
               <xsl:value-of select="$timestamp"/>
            </font>
         </td>
         <td>
            <xsl:if test="string-length(Dealer/LocationNote)!=0">
               <xsl:value-of select="Dealer/LocationNote"/>
               <br/><br/>
            </xsl:if>

            <xsl:if test="($markpro and Dealer/MerchantID!=$markproID) or $showpickup">
               <font size="+1">
                  <xsl:if test="$markpro and Dealer/MerchantID!=$markproID">
                     <xsl:text>PRO SITE:</xsl:text><br/>
                  </xsl:if>
                  <xsl:value-of select="Dealer/@Name"/>
               </font>
               <br/><br/>
            </xsl:if>

            <xsl:if test="$markpay and (Payment/Type='Pay In Store' or string-length($markprevalue)!=0)">
               <table border="0" cellspacing="0" cellpadding="0">
                  <tr>
                     <td bgcolor="#000000">
                        <font size="+2" color="#FFFFFF"><b>
                           <xsl:call-template name="LabelPayPre"/>
                        </b></font>
                     </td>
                  </tr>
               </table>
            </xsl:if>
            <!-- the spacing on this block is not the same as the others. -->
            <!-- the renderer creates an extra blank line because of the  -->
            <!-- table, but it looks nice.  the two blank lines below     -->
            <!-- were making the label larger than necessary, so I removed them. -->
         </td>
      </tr>
   </table>
</xsl:template>

<xsl:template name="SummaryBlock">
   <table border="0" cellspacing="0" cellpadding="0">
      <xsl:for-each select="Product">
         <tr>
            <td width="20"></td>
            <td><xsl:value-of select="Description"/></td>
            <td width="20"></td>
            <td align="right"><xsl:call-template name="QtyUC"/></td>
         </tr>
      </xsl:for-each>
   </table>
   <br/>
   <xsl:text>Total Prints Ordered: </xsl:text>
   <xsl:call-template name="QtySum"/><br/>
</xsl:template>

<xsl:template name="LabelAccount">
   <table border="0" cellspacing="0" cellpadding="0">
      <tr valign="top">
         <td width="200">
            <b><xsl:value-of select="Customer/BillTo/Last"/>
            <xsl:if test="string-length(Customer/BillTo/Last)!=0 or string-length(Customer/BillTo/First)!=0"><xsl:text>, </xsl:text></xsl:if>
            <xsl:value-of select="Customer/BillTo/First"/></b><br/>
            <xsl:value-of select="Customer/@ID"/><br/>
            <br/>
            <xsl:call-template name="OrderID"/><br/>
            <xsl:value-of select="Customer/BillTo/Phone"/>
         </td>
         <td>
            <xsl:if test="string-length(Dealer/LocationNote)!=0">
               <xsl:value-of select="Dealer/LocationNote"/><br/>
            </xsl:if>

            <xsl:if test="$markpro and Dealer/MerchantID!=$markproID">
               <b><xsl:text>PRO SITE:</xsl:text></b><br/>
            </xsl:if>

            <xsl:for-each select="Dealer">
               <b><xsl:value-of select="@Name"/></b><br/>
               <xsl:call-template name="DealerAddress"/>
            </xsl:for-each>

            <xsl:if test="$markpay and (Payment/Type='Pay In Store' or string-length($markprevalue)!=0)">
               <br/><br/>
               <b>
                  <xsl:call-template name="LabelPayPre"/>
               </b>
            </xsl:if>
         </td>
      </tr>
   </table>
</xsl:template>

<xsl:template name="LabelPayPre">
   <xsl:choose>
      <xsl:when test="Payment/Type='Pay In Store'">
         <xsl:call-template name="LabelPay"/>
      </xsl:when>
      <xsl:when test="string-length($markprevalue)!=0">
         <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
         <xsl:value-of select="$markprevalue"/>
         <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
      </xsl:when>
      <!-- otherwise we shouldn't be here -->
   </xsl:choose>
</xsl:template>

<xsl:template name="LabelPay">
   <xsl:text>PAY IN STORE:</xsl:text><br/>
   <xsl:if test="string-length($addmessage)!=0">
      <xsl:value-of select="$addmessage"/><br/>
   </xsl:if>
   <xsl:text>Total: </xsl:text><xsl:value-of select="$currency"/>
   <xsl:call-template name="PrimaryPaymentAmountTaxQ"/>
</xsl:template>

<xsl:template name="Invoice">

   <!-- Dealer Information -->
   <table border="0" cellspacing="0" cellpadding="0">
      <tr valign="top">
         <td width="325">
            <xsl:apply-templates select="Portal"/>
            <xsl:call-template name="LocationNote"/>
         </td>
         <td width="325" align="right">
            <xsl:call-template name="Disposition"/>
         </td>
      </tr>
      <xsl:apply-templates select="Dealer"/>
      <tr><td><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td></tr>
   </table>

   <!-- Customer Information -->
   <table border="0" cellspacing="0" cellpadding="0">
      <tr>
         <xsl:if test="$color"><xsl:attribute name="bgcolor">#00E000</xsl:attribute></xsl:if>
         <td width="325"><b>Billing Address:</b></td>
         <td width="325"><b>Shipping Address:</b></td>
      </tr>
      <xsl:apply-templates select="Customer"/>
      <tr><td><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td></tr>
   </table>

   <!-- Payment Information -->
   <table border="0" cellspacing="0" cellpadding="0">
      <xsl:apply-templates select="PaymentItem"/>
      <xsl:if test="Payment/Type!='No Primary Payment'">
         <xsl:apply-templates select="Payment"/>
      </xsl:if>
   </table>

   <!-- Special Instruction from Customer -->
   <table border="0" cellspacing="0" cellpadding="0">
      <xsl:apply-templates select="SpecialInstructions"/>
      <tr><td width="650"><hr/></td></tr>
   </table>

   <!-- Order Summary -->
   <table border="0" cellspacing="0" cellpadding="0">
      <xsl:apply-templates select="OrderSummary"/>
   </table>

   <!-- Itemize Customer's Purchase Information -->
   <xsl:if test="$items"><table border="0" cellspacing="0" cellpadding="0">

      <!-- Column Heading -->
      <tr>
         <xsl:if test="$color"><xsl:attribute name="bgcolor">#00E000</xsl:attribute></xsl:if>
         <td width="100"><b><u>File</u></b></td>
         <td width="240"><b><u>Image Description</u></b></td>
         <td width="180"><b><u>Sku</u></b></td>
         <td width="30" align="right"><b><u>Qty</u></b></td>
         <td width="50" align="right"><b><u>Price</u></b></td>
         <td width="50" align="right"><b><u>Total</u></b></td>
      </tr>

      <!-- Print out each item -->
      <xsl:apply-templates select="Items"/>
      <xsl:apply-templates select="SplitOrderProducts"/>

      <tr><td><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td></tr>
   </table></xsl:if>

   <!-- Additional Items -->
   <xsl:apply-templates select="Addons"/>

   <!-- Final Delimiter Bar -->
   <table border="0" cellspacing="0" cellpadding="0">

      <!-- Column Heading -->
      <tr>
         <xsl:if test="$color"><xsl:attribute name="bgcolor">#00E000</xsl:attribute></xsl:if>
         <td width="650"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
      </tr>

      <tr><td><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td></tr>
   </table>

   <!-- Totals Block -->
   <table border="0" cellspacing="0" cellpadding="0">

      <!-- Subtotal -->
      <tr>
         <td width="420"></td>
         <td width="130" colspan="2"><font size="-1"><b>Subtotal:</b></font></td>
         <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/><xsl:value-of select="SubTotal"/></b></font></td>
      </tr>

      <!-- Shipping -->
      <tr>
         <td></td>
         <td width="130" colspan="2"><font size="-1"><b>Shipping:</b></font></td>
         <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/>
         <!-- if there's a ShippingCost field, use that, otherwise just Shipping -->
            <xsl:choose>
               <xsl:when test="count(ShippingCost)>0">
                  <xsl:value-of select="ShippingCost"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:value-of select="Shipping"/>
               </xsl:otherwise>
            </xsl:choose>
         </b></font></td>
      </tr>

      <!-- Special Discount -->
      <xsl:apply-templates select="Discount"/>

      <!-- Tax -->
      <xsl:if test="not(TaxIsGST='true')"><!-- handles null correctly -->
         <tr>
            <td></td>
            <td width="130" colspan="2"><font size="-1"><b>Tax: (<xsl:value-of select="TaxRate"/><xsl:if test="TaxRate!='EXEMPT'">%</xsl:if>)</b></font></td>
            <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/><xsl:value-of select="Tax"/></b></font></td>
         </tr>
      </xsl:if>

      <tr>
         <td></td>
         <td width="230" colspan="3"><hr/></td>
      </tr>

      <!-- Grand Total -->
      <tr>
         <td></td>
         <td width="130" colspan="2"><font size="-1"><b>Grand Total:</b></font></td>
         <td width="100" align="right"><font size="+2"><b><xsl:value-of select="$currency"/><xsl:value-of select="GrandTotal"/></b></font></td>
      </tr>

      <!-- GST -->
      <xsl:if test="TaxIsGST='true'">
         <tr>
            <td></td>
            <td width="130" colspan="2"><font size="-1"><b>GST Included In Total:</b></font></td>
            <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/><xsl:value-of select="Tax"/></b></font></td>
         </tr>
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="110"><font size="-1"><b>(<xsl:value-of select="TaxRate"/><xsl:if test="TaxRate!='EXEMPT'">%</xsl:if>)</b></font></td>
         </tr>
      </xsl:if>

      <!-- Payment Items -->
      <xsl:for-each select="PaymentItem">
         <tr>
            <td></td>
            <td width="130" colspan="2"><font size="-1"><b><xsl:value-of select="LineItem"/>:</b></font></td>
            <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/><xsl:value-of select="Amount"/></b></font></td>
         </tr>
      </xsl:for-each>

      <!-- Primary Payment -->
      <xsl:if test="Payment/Type!='Pay In Store' and Payment/Type!='On Account' and Payment/Type!='No Primary Payment'">
         <tr>
            <td></td>
            <td width="130" colspan="2"><font size="-1"><b>
               <xsl:choose>
                  <xsl:when test="Payment/Type='CC'">Amount Charged to Credit Card:</xsl:when>
                  <xsl:when test="Payment/Type='PP'">Amount Paid through PayPal:</xsl:when>
                  <xsl:when test="Payment/Type='SC'">Amount Charged to Star Card:</xsl:when>
                  <xsl:otherwise>Amount Paid:</xsl:otherwise>
               </xsl:choose>
            </b></font></td>
            <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/>
               <xsl:call-template name="PrimaryPaymentAmount"/>
            </b></font></td>
         </tr>
      </xsl:if>

      <!-- Balance Due -->
      <tr>
         <td></td>
         <td width="130" colspan="2"><font size="-1"><b>Balance Due:</b></font></td>
         <td width="100" align="right"><font size="-1"><b><xsl:value-of select="$currency"/>
            <xsl:choose>
               <xsl:when test="Payment/Type='Pay In Store' or Payment/Type='On Account'">
                  <xsl:call-template name="PrimaryPaymentAmount"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>0.00</xsl:text><!-- be completely clear that nothing is due -->
               </xsl:otherwise>
            </xsl:choose>
         </b></font></td>
      </tr>

   </table>

</xsl:template>

<xsl:template match="Portal">
   <b><font size="+1" face="Tahoma"><xsl:value-of select="@Name"/></font></b>
</xsl:template>

<xsl:template name="Disposition">
   <xsl:if test="$disposition">
      <b><font size="+2">
         <xsl:choose>
            <xsl:when test="$home">
               <xsl:text>Ship to Customer</xsl:text>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="Dealer/@Name"/>
            </xsl:otherwise>
         </xsl:choose>
      </font></b>
   </xsl:if>
</xsl:template>

<xsl:template name="OrderID">
   <xsl:text>Invoice: </xsl:text>
   <xsl:value-of select="@ID"/>
   <xsl:if test="string-length($lifepicsID)!=0">
      <br/>
      <xsl:text>Global Order #</xsl:text>
      <xsl:value-of select="$lifepicsID"/>
   </xsl:if>
</xsl:template>

<xsl:template match="Dealer">
   <tr valign="top">
      <td width="325">
         <table border="0" cellspacing="0" cellpadding="0">
            <tr valign="top">
               <xsl:if test="string-length($logo)!=0 and not (string-length($markproID)!=0 and MerchantID!=$markproID)"><td>
                  <img>
                     <xsl:attribute name="src"><xsl:value-of select="$logo"/></xsl:attribute>
                     <xsl:if test="string-length($logowidth)!=0">
                        <xsl:attribute name="width"><xsl:value-of select="$logowidth"/></xsl:attribute>
                     </xsl:if>
                     <xsl:if test="string-length($logoheight)!=0">
                        <xsl:attribute name="height"><xsl:value-of select="$logoheight"/></xsl:attribute>
                     </xsl:if>
                  </img>
                  <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
               </td></xsl:if>
               <td><b>
                  <xsl:value-of select="@Name"/>
                  <xsl:if test="string-length(../Affiliate)!=0">
                     <xsl:text> - </xsl:text><xsl:value-of select="../Affiliate"/>
                  </xsl:if><br/>
                  <xsl:call-template name="DealerAddress"/>
               </b></td>
            </tr>
         </table>
      </td>
      <td width="325" align="right"><b>
         <font>
            <xsl:if test="$enlargetype"><xsl:attribute name="size">+2</xsl:attribute></xsl:if>
            <xsl:for-each select="..">
               <xsl:call-template name="OrderID"/>
            </xsl:for-each>
         </font><br/>
         <xsl:value-of select="$timestamp"/>
      </b></td>
   </tr>
</xsl:template>

<xsl:template name="DealerAddress">
   <xsl:value-of select="Street1"/><br/>
   <!-- dealer has a street2, but we haven't been using it -->
   <xsl:value-of select="City"/>
   <xsl:if test="string-length(City)!=0 and string-length(State)!=0"><xsl:text>, </xsl:text></xsl:if>
   <xsl:value-of select="State"/>
   <xsl:text> </xsl:text>
   <xsl:value-of select="ZIP"/>
</xsl:template>

<xsl:template name="OverrideAddress">
   <xsl:value-of select="$ovstreet1"/><br/>
   <xsl:if test="string-length($ovstreet2)!=0">
      <xsl:value-of select="$ovstreet2"/><br/>
   </xsl:if>
   <xsl:value-of select="$ovcity"/>
   <xsl:if test="string-length($ovcity)!=0 and string-length($ovstate)!=0"><xsl:text>, </xsl:text></xsl:if>
   <xsl:value-of select="$ovstate"/>
   <xsl:text> </xsl:text>
   <xsl:value-of select="$ovzip"/>
</xsl:template>

<xsl:template match="Customer">
   <tr valign="top">
      <td width="325"><font size="-1">
         <xsl:apply-templates select="BillTo"/>
         <!-- Only show email address if NOT a Portal -->
         <xsl:if test="string-length(../Portal/@Name)=0">
            <br/><xsl:value-of select="@ID"/>
         </xsl:if>
      </font></td>
      <td width="325"><font size="-1">
         <xsl:apply-templates select="ShipTo"/>
         <xsl:if test="string-length(../Dealer/LocationNote)!=0 and not($disposition)">
            <br/><xsl:value-of select="../Dealer/LocationNote"/>
         </xsl:if>
      </font></td>
   </tr>
</xsl:template>

<xsl:template name="LocationNote">
   <xsl:if test="string-length(Dealer/LocationNote)!=0 and $disposition">
      <b><font size="+2"><xsl:value-of select="Dealer/LocationNote"/></font></b>
   </xsl:if>
</xsl:template>

<xsl:template match="BillTo">
   <font size="+2"><b><xsl:call-template name="CustomerName"/></b></font><br/>
   <xsl:call-template name="CustomerAddress"/><br/>
   <xsl:value-of select="Phone"/>
</xsl:template>

<xsl:template match="ShipTo">
   <xsl:if test="string-length(Street1)=0">
      <xsl:call-template name="ShippingBlock"/><br/>
   </xsl:if>
   <xsl:call-template name="CustomerName"/><br/>
   <xsl:call-template name="CustomerAddress"/><br/>
   <xsl:value-of select="Phone"/>
   <xsl:if test="(string-length(Street1)!=0)and((count(../../OrderSummary/ShipMethod)>0)or(string-length(Comment)!=0))">
      <br/><xsl:call-template name="ShippingBlock"/>
   </xsl:if>
</xsl:template>

<xsl:template name="ShippingBlock">
   <xsl:choose>
      <xsl:when test="count(../../OrderSummary/ShipMethod)>0">
         <xsl:for-each select="../../OrderSummary">
            <xsl:if test="position()!=1"><br/></xsl:if>
            <xsl:value-of select="ShipMethod"/>
         </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="Comment"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template name="CustomerName">
   <xsl:value-of select="First"/><xsl:text> </xsl:text>
   <xsl:value-of select="Last"/>
</xsl:template>
<xsl:template name="CustomerAddress">
   <xsl:if test="string-length(Company)!=0">
      <xsl:value-of select="Company"/><br/>
   </xsl:if>
   <xsl:value-of select="Street1"/><br/>
   <xsl:if test="string-length(Street2)!=0">
      <xsl:value-of select="Street2"/><br/>
   </xsl:if>
   <xsl:if test="string-length(AltCSZ)!=0">
      <xsl:value-of select="AltCSZ"/>
      <xsl:text> </xsl:text>
      <!-- if AltCSZ is present, CSZ should all be blank, but add a space just in case -->
   </xsl:if>
   <xsl:value-of select="City"/>
   <xsl:if test="string-length(City)!=0 and string-length(State)!=0"><xsl:text>, </xsl:text></xsl:if>
   <xsl:value-of select="State"/>
   <xsl:text> </xsl:text>
   <xsl:value-of select="ZIP"/><br/>
   <xsl:value-of select="Country"/>
</xsl:template>

<xsl:template match="Items">
   <xsl:call-template name="ItemBlock"/>
</xsl:template>

<xsl:template match="SplitOrderProducts">
   <xsl:call-template name="ItemBlock"/>
</xsl:template>

<xsl:template name="ItemBlock">
   <xsl:for-each select="Item">
      <tr>
         <!-- this is the only place the image fields are used -->
         <xsl:choose>
            <xsl:when test="count(Images)>0">
               <td width="100"><font size="-1"><xsl:value-of select="Images/Image/OrigImageName"/></font></td>
               <td width="240"><font size="-1">[<xsl:value-of select="@ID"/>]<xsl:text> </xsl:text><xsl:value-of select="Images/Image/ImageDescription"/><xsl:text> </xsl:text>(<xsl:value-of select="ProductDescription"/>)</font></td>
            </xsl:when>
            <xsl:otherwise>
               <td width="100"><font size="-1"><xsl:value-of select="OrigImageName"/></font></td>
               <td width="240"><font size="-1">[<xsl:value-of select="@ID"/>]<xsl:text> </xsl:text><xsl:value-of select="ImageDescription"/><xsl:text> </xsl:text>(<xsl:value-of select="ProductDescription"/>)</font></td>
            </xsl:otherwise>
         </xsl:choose>
         <td width="180"><font size="-1"><xsl:value-of select="ProductSku"/></font></td>
         <td width="30" align="right"><font size="-1"><xsl:call-template name="QtyLC"/></font></td>
         <td width="50" align="right"><font size="-1"><xsl:value-of select="$currency"/><xsl:value-of select="Price"/></font></td>
         <td width="50" align="right"><font size="-1"><xsl:value-of select="$currency"/><xsl:value-of select="Total"/></font></td>
      </tr>
      <xsl:if test="string-length(GiftText)!=0">
         <tr>
            <td></td>
            <td width="500" colspan="4"><font size="-1">
               "<xsl:value-of select="GiftText"/>"
            </font></td>
         </tr>
      </xsl:if>
      <xsl:if test="string-length(ResolutionWarning)!=0">
         <tr>
            <td></td>
            <td width="500" colspan="4"><font size="-1">
               <xsl:if test="$color"><xsl:attribute name="color">#800000</xsl:attribute></xsl:if>
               <b>Image Resolution Lower Than Recommended!<br/>
               <xsl:value-of select="ResolutionWarning"/></b>
            </font></td>
         </tr>
      </xsl:if>
      <xsl:if test="string-length(SplitOrderPrintAtLocation)!=0">
         <tr>
            <td></td>
            <td width="500" colspan="4"><font size="-1">
               <xsl:if test="$color"><xsl:attribute name="color">#800000</xsl:attribute></xsl:if>
               <b><xsl:value-of select="SplitOrderPrintAtLocation"/></b>
            </font></td>
         </tr>
      </xsl:if>
   </xsl:for-each>
</xsl:template>

<xsl:template match="Addons">
   <xsl:if test="count(AddonItem)>0"><table border="0" cellspacing="0" cellpadding="0">

      <!-- Column Heading -->
      <tr>
         <xsl:if test="$color"><xsl:attribute name="bgcolor">#00E000</xsl:attribute></xsl:if>
         <td width="340" colspan="2"><b>Additional Items</b></td>
         <td width="180"><b><u>Sku</u></b></td>
         <td width="30" align="right"><b><u>Qty</u></b></td>
         <td width="50" align="right"><b><u>Price</u></b></td>
         <td width="50" align="right"><b><u>Total</u></b></td>
      </tr>

      <!-- Print out each item -->
      <xsl:for-each select="AddonItem">
         <tr>
            <td width="100"><font size="-1"><xsl:value-of select="Pages/Page/OrigImageName"/></font></td>
            <td width="240"><font size="-1"><xsl:value-of select="Name"/></font></td>
            <td width="180"><font size="-1"><xsl:value-of select="SKU"/></font></td>
            <td width="30" align="right"><font size="-1"><xsl:call-template name="QtyLC"/></font></td>
            <td width="50" align="right"><font size="-1"><xsl:value-of select="$currency"/><xsl:value-of select="Price"/></font></td>
            <td width="50" align="right"><font size="-1"><xsl:value-of select="$currency"/><xsl:value-of select="Total"/></font></td>
         </tr>
      </xsl:for-each>

      <tr><td><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td></tr>
   </table></xsl:if>
</xsl:template>

<xsl:template match="Discount">
   <xsl:if test="FreePrints + FreeShipping + Credit + PercentDiscount + PermanentDiscount !=0">
      <tr>
         <td></td>
         <td width="130" colspan="2"><font size="-1"><b>Discount</b></font></td>
      </tr>
      <xsl:if test="FreePrints!=0">
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="110"><font size="-1">Free Prints:</font></td>
            <td width="100" align="right"><font size="-1"><b>- <xsl:value-of select="$currency"/><xsl:value-of select="FreePrints"/></b></font></td>
         </tr>
      </xsl:if>
      <xsl:if test="FreeShipping!=0">
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="110"><font size="-1">Free Shipping:</font></td>
            <td width="100" align="right"><font size="-1"><b>- <xsl:value-of select="$currency"/><xsl:value-of select="FreeShipping"/></b></font></td>
         </tr>
      </xsl:if>
      <xsl:if test="Credit!=0">
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="110"><font size="-1">Credit:</font></td>
            <td width="100" align="right"><font size="-1"><b>- <xsl:value-of select="$currency"/><xsl:value-of select="Credit"/></b></font></td>
         </tr>
      </xsl:if>
      <xsl:if test="PercentDiscount!=0">
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="110"><font size="-1">% Discount:</font></td>
            <td width="100" align="right"><font size="-1"><b>- <xsl:value-of select="$currency"/><xsl:value-of select="PercentDiscount"/></b></font></td>
         </tr>
      </xsl:if>
      <xsl:if test="PermanentDiscount!=0">
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="110"><font size="-1">% Discount:</font></td>
            <td width="100" align="right"><font size="-1"><b>- <xsl:value-of select="$currency"/><xsl:value-of select="PermanentDiscount"/></b></font></td>
         </tr>
      </xsl:if>
      <xsl:for-each select="PromoCode">
         <tr>
            <td></td>
            <td width="20"></td>
            <td width="210" colspan="2"><font size="-1">Code: <xsl:value-of select="."/></font></td>
         </tr>
      </xsl:for-each>
   </xsl:if>
</xsl:template>

<xsl:template match="PaymentItem">
   <tr valign="top">
      <td width="550"><hr/><b>
         <font size="+2">
            <xsl:text>Payment Type: </xsl:text>
            <xsl:value-of select="Header"/>
         </font>
         <font size="-1">
            <xsl:for-each select="Detail">
               <br/><xsl:value-of select="."/>
            </xsl:for-each>
         </font>
      </b></td>
      <td width="100" align="right"><hr/><b>
         <font size="+2">
            <xsl:value-of select="$currency"/>
            <xsl:value-of select="Amount"/>
         </font>
      </b></td>
   </tr>
</xsl:template>

<xsl:template match="Payment">
   <tr valign="top">
      <td width="550"><hr/><b>
         <font size="+2">
            <xsl:text>Payment Type: </xsl:text>
            <xsl:if test="Type!='Pay In Store' and Type!='On Account'">Prepaid - </xsl:if>
            <xsl:value-of select="Type"/><xsl:text> </xsl:text>
            <xsl:value-of select="CardType"/><xsl:text> </xsl:text>
            <xsl:value-of select="Account"/><br/>
         </font>
         <font size="-1">
            <xsl:if test="string-length(Expiration)!=0">
               <xsl:text>Expiration: </xsl:text><xsl:value-of select="Expiration"/><br/>
            </xsl:if>
            <xsl:if test="string-length(Authorization)!=0">
               <xsl:text>Authorization: </xsl:text><xsl:value-of select="Authorization"/>
            </xsl:if>
         </font>
      </b></td>
      <td width="100" align="right"><hr/><b>
         <font size="+2">
            <xsl:value-of select="$currency"/>
            <xsl:for-each select="..">
               <xsl:call-template name="PrimaryPaymentAmount"/>
            </xsl:for-each>
         </font>
      </b></td>
   </tr>
</xsl:template>

<xsl:template match="SpecialInstructions">
   <xsl:if test="string-length(Message)!=0">
      <tr>
         <td width="650"><hr/><b>Special Instructions:</b></td>
      </tr>
      <tr>
         <xsl:if test="$color"><xsl:attribute name="bgcolor">#FFFF00</xsl:attribute></xsl:if>
         <td width="650"><font size="-1"><xsl:value-of select="Message"/></font></td>
      </tr>
   </xsl:if>
</xsl:template>

<xsl:template match="OrderSummary">
   <xsl:choose>
      <xsl:when test="count(ShipMethod)>0">
         <tr>
            <xsl:if test="$color"><xsl:attribute name="bgcolor">#00E000</xsl:attribute></xsl:if>
            <td width="650" colspan="7"><b>Order Summary - <xsl:value-of select="ShipMethod"/></b></td>
         </tr>
      </xsl:when>
      <xsl:otherwise>
         <tr>
            <xsl:if test="$color"><xsl:attribute name="bgcolor">#00E000</xsl:attribute></xsl:if>
            <td width="200"><b>Order Summary</b></td>
            <td width="30" align="right"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
            <td width="10"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
            <td width="180"><b><u>Sku</u></b></td>
            <td width="50" align="right"><b><u>Total</u></b></td>
            <td width="10"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
            <td width="170"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
         </tr>
      </xsl:otherwise>
   </xsl:choose>
   <xsl:for-each select="Product">
      <tr valign="top">
         <td width="200"><font size="-1"><b>Number of <xsl:value-of select="Description"/>s:</b></font></td>
         <td width="30" align="right"><font size="-1"><b><xsl:call-template name="QtyUC"/></b></font></td>
         <td width="10"></td>
         <td width="180"><font size="-1"><b><xsl:value-of select="SKU"/></b></font></td>
         <td width="50" align="right"><font size="-1"><b><xsl:value-of select="$currency"/><xsl:value-of select="Subtotal"/></b></font></td>
         <td width="10"></td>
         <td width="170"><font size="-1"><xsl:value-of select="Note"/></font></td>
      </tr>
      <xsl:if test="count(Barcode)>0">
         <tr>
            <td width="650" colspan="7">
               <table cellpadding="0" cellspacing="0" border="0">
                  <tr>
                     <td width="20" height="20"></td>
                     <xsl:apply-templates select="Barcode"/>
                     <td width="5"></td>
                     <td><xsl:value-of select="Barcode/d"/></td>
                  </tr>
               </table>
            </td>
         </tr>
      </xsl:if>
   </xsl:for-each>
   <tr><td><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td></tr>
</xsl:template>

<xsl:template match="Barcode">
   <xsl:apply-templates select="o|l|e"/>
</xsl:template>

<xsl:template match="o"><td bgcolor="#FFFFFF" width="1"></td></xsl:template>
<xsl:template match="l"><td bgcolor="#000000" width="1"></td></xsl:template>
<xsl:template match="e">
   <td>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
      <xsl:choose>
         <xsl:when test=".=2">
            NO BARCODE
         </xsl:when>
         <xsl:otherwise>
            BARCODE ERROR <xsl:value-of select="."/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
   </td>
</xsl:template>

<!-- templates to deal with display quantity vs. actual quantity -->
<!-- an example: actual quantity 100 might come from display quantity 5 of a 20-pack product -->
<!-- XSLT is case-sensitive, but I prefer the explicit UC and LC -->
<!-- I'm not sure DisplayQty is sent in all cases, but it doesn't do any harm to look for it -->

<xsl:template name="QtyUC">
   <xsl:choose>
      <xsl:when test="string-length(DisplayQty)!=0">
         <xsl:value-of select="DisplayQty"/>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="QTY"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template name="QtyLC">
   <xsl:choose>
      <xsl:when test="string-length(DisplayQty)!=0">
         <xsl:value-of select="DisplayQty"/>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="Qty"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<!-- if DisplayQty appears, it appears on all nodes, so this works -->
<xsl:template name="QtySum">
   <xsl:choose>
      <xsl:when test="count(Product/DisplayQty)>0">
         <xsl:value-of select="sum(Product/DisplayQty)"/>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="sum(Product/QTY)"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<!-- templates to deal with new vs. old payment amounts -->
<!-- code for GiftCard was similar but has been removed -->

<!-- do not call when Payment/Type='No Primary Payment' -->
<!-- but Payment/Amount is filled in even in that case  -->

<xsl:template name="PrimaryPaymentAmount">
   <xsl:choose>
      <xsl:when test="count(Payment/Amount)>0">
         <xsl:value-of select="Payment/Amount"/>
      </xsl:when>
      <xsl:otherwise>
         <xsl:value-of select="GrandTotal"/>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template name="PrimaryPaymentAmountTaxQ">
   <xsl:choose>
      <xsl:when test="count(Payment/Amount)>0">
         <xsl:if test="$includetax">
            <xsl:value-of select="Payment/Amount"/>
         </xsl:if>
         <xsl:if test="not($includetax)">
            <xsl:value-of select="format-number((GrandTotal - Tax)*(Payment/Amount div GrandTotal),'0.00')"/>
         </xsl:if>
         <!-- note that the spaces around "-" are important for the correct meaning! -->
      </xsl:when>
      <xsl:otherwise>
         <xsl:if test="$includetax">
            <xsl:value-of select="GrandTotal"/>
         </xsl:if>
         <xsl:if test="not($includetax)">
            <xsl:value-of select="format-number(GrandTotal - Tax,'0.00')"/>
         </xsl:if>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template>

</xsl:stylesheet>

