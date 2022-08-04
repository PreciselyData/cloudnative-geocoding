#!/bin/bash
# NOTE: Assumes $CATALINA_HOME and $GTX_DATA are set

GTX_HOME="${0%/*}"

### Available CPUs for container
CPUs=1 
### Overrite it with user's input, if user set it
if [[ ! -z "$POD_CPU_LIMIT" ]] && (($(echo $POD_CPU_LIMIT '>' 0|bc))); then
      CPUs=$POD_CPU_LIMIT
 
fi

### Using ceiling value of [ CPUs * 1.5 ]
### For less than 1 CPU, we are creating single instance of geostan
maxPoolActive=`echo "($CPUs * 1.5) + 0.5" | bc`
### Convert decimal to integer
maxPoolActive=${maxPoolActive%.*}  

### Just to make sure that we are getting at least one geostan instance
if [[ -z "$maxPoolActive" ]] || (($(echo $maxPoolActive '==' 0|bc))); then
  maxPoolActive=1
fi

if [[ ! -z "$GTX_POOL_MAX_ACTIVE" ]] && (( $GTX_POOL_MAX_ACTIVE > 0 )); then
	   maxPoolActive=$GTX_POOL_MAX_ACTIVE
fi

remote="false"

if [[ "${GTX_REMOTE,,}" == "true" ]]; then
    remote="true"
fi

(cd $GTX_HOME/cli; sh ./cli.sh configure --s $GTX_DATA --d $GTX_HOME/resources/config)
(cd $GTX_HOME/cli; sh ./cli.sh setting --d $GTX_HOME/resources/config --t USA --n REMOTE --v $remote)
(cd $GTX_HOME/cli; sh ./cli.sh setting --d $GTX_HOME/resources/config --t USA --n POOL_MAX_ACTIVE --v $maxPoolActive)
 echo "Deploying the Geotax application with settings: [REMOTE:$remote, POOL_MAX_ACTIVE:$maxPoolActive]"

# Need to start tomcat because we overrode the tomcat startup CMD in our dockerfile
catalina.sh run