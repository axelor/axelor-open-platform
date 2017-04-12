/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
{
    "model" : "com.axelor.test.db.Contact",
    "fields": ["fullName", "email"],
    "data":{
    	"_domain": "self.title.code = 'mr'",
        "operator":"and", 
        "criteria":[
            {
                "fieldName":"code", 
                "operator":"equals", 
                "value":"er"
            }, 
            {
                "fieldName":"name", 
                "operator":"equals", 
                "value":"er"
            }, 
            {
                "operator":"or", 
                "criteria":[
                    {
                        "fieldName":"name", 
                        "operator":"equals", 
                        "value":"we"
                    }, 
                    {
                        "fieldName":"code", 
                        "operator":"between", 
                        "start":"er", 
                        "end":"wer"
                    }
                ]
            },
            {
                "operator":"or", 
                "criteria":[
                    {
                        "fieldName":"name", 
                        "operator":"equals", 
                        "value":"we"
                    }, 
                    {
                        "fieldName":"lang", 
                        "operator":"in", 
                        "value": ["en", "hi"]
                    }
                ]
            }
        ]
    }, 
    "offset":0, 
    "limit":75
}