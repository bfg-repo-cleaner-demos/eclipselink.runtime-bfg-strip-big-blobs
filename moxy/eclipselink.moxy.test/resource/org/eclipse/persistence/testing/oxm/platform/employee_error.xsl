<?xml version="1.0"?>
<xsl:not_a_valid_tag xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:template match="employee">
		<e>
			<fn><xsl:value-of select="first-name"/></fn>
			<ln><xsl:value-of select="last-name"/></ln>
		</e>
	</xsl:template>

</xsl:not_a_valid_tag>
