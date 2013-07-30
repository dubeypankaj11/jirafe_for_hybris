/**
 * 
 */
package org.jirafe.converter;

import de.hybris.platform.util.Config;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * @author dbrand
 * 
 */
@SuppressWarnings("all")
public class UTCFormatter
{
	private final static SimpleDateFormat utcFormatter;
	static
	{
		utcFormatter = new SimpleDateFormat(Config.getString("jirafe.isoFormatString", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	private final static SimpleDateFormat localFormatter;
	static
	{
		localFormatter = new SimpleDateFormat(Config.getString("jirafe.localFormatString", "yyyy-MM-dd'T'HH:mm:ss.SSS"));
	}

	public static DateFormat getUTCFormatter()
	{
		return utcFormatter;
	}

	public static String format(final Date date)
	{
		return utcFormatter.format(date);
	}

	public static Date parse(final String s) throws ParseException
	{
		return utcFormatter.parse(s);
	}

	public static String toLocal(final String utc) throws ParseException
	{
		return localFormatter.format(utcFormatter.parse(utc));
	}
}
