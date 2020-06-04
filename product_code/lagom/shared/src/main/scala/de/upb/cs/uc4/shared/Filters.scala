package de.upb.cs.uc4.shared

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

class Filters @Inject() (corsFilter: CORSFilter) extends DefaultHttpFilters(corsFilter)
